package org.humanbrainproject.knowledgegraph.structure.boundary;

import com.arangodb.ArangoDBException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInferredRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoStructureRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoToNexusLookupMap;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.structure.exceptions.AsynchronousStartupDelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
@Component
public class Structure {


    @Autowired
    ArangoStructureRepository structureRepository;

    @Autowired
    ArangoInternalRepository internalRepository;

    @Autowired
    ArangoInferredRepository inferredRepository;

    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    @Autowired
    ArangoToNexusLookupMap lookupMap;

    @Autowired
    CacheManager cacheManager;

    protected Logger logger = LoggerFactory.getLogger(Structure.class);

    private static boolean STRUCTURE_LOCK = false;


    private Map<String, Map> groupDirectReferences(List<Map> relations, boolean outbound) {
        Map<String, Map> groupedLinks = new HashMap<>();
        relations.forEach(r -> {
            if (r != null && r.get("attribute") != null && r.get("ref") != null) {
                Map attribute = (Map) groupedLinks.get(r.get("attribute"));
                if (attribute == null) {
                    Map map = new HashMap();
                    map.put("attribute", r.get("attribute").toString());
                    map.put("simplePropertyName", semanticsToHumanTranslator.extractSimpleAttributeName(r.get("attribute").toString()));
                    map.put("label", semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel(r.get("attribute").toString()));
                    if (!outbound) {
                        map.put("reverse", true);
                    }
                    groupedLinks.put(r.get("attribute").toString(), map);
                    attribute = map;
                }
                attribute.computeIfAbsent("canBe", k -> new ArrayList<String>());
                List<String> canBe = (List<String>) attribute.get("canBe");
                NexusSchemaReference ref = lookupMap.getNexusSchema(new ArangoCollectionReference(r.get("ref").toString()));
                if(ref!=null){
                    canBe.add(ref.getRelativeUrl().getUrl());
                }
            }
        });
        return groupedLinks;
    }


    public JsonDocument getStructureForSchema(NexusSchemaReference schemaReference, boolean withLinks) {
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.put("id", schemaReference.getRelativeUrl().getUrl());
        jsonDocument.put("group", schemaReference.getOrganization());
        jsonDocument.put("label", semanticsToHumanTranslator.translateNexusSchemaReference(schemaReference));
        //TODO reflect on schema
        ArangoCollectionReference arangoReference = ArangoCollectionReference.fromNexusSchemaReference(schemaReference);
        if (!inferredRepository.hasInstances(arangoReference)) {
            return null;
        }
        Map<String, Map> inboundRelations;
        if (withLinks) {
            inboundRelations = groupDirectReferences(structureRepository.getDirectRelationsWithType(arangoReference, false), false);
        } else {
            inboundRelations = Collections.emptyMap();
        }
        List<Map> attributesWithCount = structureRepository.getAttributesWithCount(arangoReference);
        Map<String, Map> outboundRelations;
        if (attributesWithCount.size() > 0 && withLinks) {
            outboundRelations = groupDirectReferences(structureRepository.getDirectRelationsWithType(arangoReference, true), true);
        } else {
            outboundRelations = Collections.emptyMap();
        }

        attributesWithCount.forEach(map -> {
            Object attribute = map.get("attribute");
            if (attribute != null) {
                Map outboundRelation = outboundRelations.get(attribute);
                if (outboundRelation != null) {
                    map.put("canBe", outboundRelation.get("canBe"));
                }
                map.put("simpleAttributeName", semanticsToHumanTranslator.extractSimpleAttributeName(attribute.toString()));
                map.put("label", semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel(attribute.toString()));
            }
        });
        inboundRelations.values().forEach(a ->
                attributesWithCount.add(a)
        );
        if (attributesWithCount.isEmpty()) {
            return null;
        }
        jsonDocument.put("properties", attributesWithCount);
        return jsonDocument;
    }

    @Cacheable(value="structure", key="#withLinks")
    public JsonDocument getCachedStructure(boolean withLinks){
        if(STRUCTURE_LOCK){
            throw new AsynchronousStartupDelay();
        }
        return getStructure(withLinks);
    }


    @CachePut(value="structure", key="#withLinks")
    public JsonDocument refreshStructureCache(boolean withLinks){
        if(STRUCTURE_LOCK){
            throw new AsynchronousStartupDelay();
        }
        logger.info(String.format("Refreshing the cache for structure queries %s", withLinks ? "with links" : "without links"));
        JsonDocument structure = getStructure(withLinks);
        logger.info(String.format("Done refreshing the cache for structure queries %s", withLinks ? "with links" : "without links"));
        return structure;
    }

    private static final int DAY_IN_MS = 24*60*60*1000;

    @Scheduled(fixedDelay = DAY_IN_MS)
    public void refreshStructureCachesEveryDay(){
        try {
            logger.info("CRON: Refreshing the cache for structure queries");
            refreshStructureCache(false);
            refreshStructureCache(true);
            logger.info("CRON: Done refreshing the cache for structure queries");
        }
        catch(AsynchronousStartupDelay e){
            logger.info("CRON: Waiting for initial cache population to end");

        }
    }

    @Async
    public void refreshStructuredCachesAtStartup(){
        STRUCTURE_LOCK = true;
        logger.info("Initial population of the cache for structure queries started");

        cacheManager.getCache("structure").put(false, getStructure(false));
        cacheManager.getCache("structure").put(true, getStructure(true));

        logger.info("Done with initial population of the cache for structure queries");
        STRUCTURE_LOCK = false;
    }


    private JsonDocument getStructure(boolean withLinks) {
        Collection<NexusSchemaReference> allSchemas = lookupMap.getLookupTable(false).values();
        JsonDocument jsonDocument = new JsonDocument();
        List<JsonDocument> schemas = new ArrayList<>();
        jsonDocument.put("schemas", schemas);
        for (NexusSchemaReference schemaReference : allSchemas) {
            try {
                logger.debug(String.format("fetching structure from schema %s", schemaReference.getRelativeUrl().getUrl()));
                JsonDocument structureForSchema = getStructureForSchema(schemaReference, withLinks);
                if (structureForSchema != null) {
                    schemas.add(structureForSchema);
                }
            } catch (ArangoDBException exception) {
                JsonDocument document = new JsonDocument();
                document.put("id", schemaReference.getRelativeUrl().getUrl());
                document.put("failure", String.format("Was not able to reflect. Cause: ", exception.getErrorMessage()));
                schemas.add(document);
            }
        }
        return jsonDocument;
    }

    public void reflectOnSpecifications(NexusSchemaReference schemaReference) {
        String prefix = ArangoCollectionReference.fromNexusSchemaReference(schemaReference).getName()+"-";
        List<Map> internalDocuments = internalRepository.getInternalDocumentsWithKeyPrefix(ArangoQuery.SPECIFICATION_QUERIES, prefix, Map.class);
        System.out.println(internalDocuments);
    }

    public List<String> getArangoEdgeCollections() {
        return inferredRepository.getCollectionNames().stream().map(ArangoCollectionReference::getName).collect(Collectors.toList());
    }


}
