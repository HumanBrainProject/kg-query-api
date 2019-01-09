package org.humanbrainproject.knowledgegraph.structure.boundary;

import com.arangodb.ArangoDBException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.*;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
@Component
public class Structure {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoStructureRepository structureRepository;

    @Autowired
    ArangoInternalRepository internalRepository;

    @Autowired
    ArangoInferredRepository inferredRepository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    NexusClient nexusClient;


    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    @Autowired
    ArangoToNexusLookupMap lookupMap;


    public Set<NexusSchemaReference> getAllSchemasInMainSpace() {
        Set<NexusSchemaReference> allSchemas = nexusClient.getAllSchemas(null, null, authorizationContext.getInterceptor());
        return allSchemas.stream().map(s -> s.toSubSpace(SubSpace.MAIN)).collect(Collectors.toSet());
    }

    private Map<String, Map> groupDirectReferences(List<Map> relations, boolean outbound) {
        Map<String, Map> groupedLinks = new HashMap<>();
        relations.forEach(r -> {
            if (r != null && r.get("attribute") != null && r.get("ref") != null) {
                Map attribute = (Map) groupedLinks.get(r.get("attribute"));
                if (attribute == null) {
                    Map map = new HashMap();
                    map.put("attribute", r.get("attribute").toString());
                    map.put("simplePropertyName", semanticsToHumanTranslator.extractSimpleAttributeName(r.get("attribute").toString()));
                    map.put("canBe", new ArrayList<String>());
                    map.put("label", semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel(r.get("attribute").toString()));
                    if (!outbound) {
                        map.put("reverse", true);
                    }
                    groupedLinks.put(r.get("attribute").toString(), map);
                    attribute = map;
                }
                List<String> canBe = (List<String>) attribute.get("canBe");
                canBe.add(lookupMap.getNexusSchema(new ArangoCollectionReference(r.get("ref").toString())).getRelativeUrl().getUrl());
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

    public JsonDocument getStructure(boolean withLinks) {
        Collection<NexusSchemaReference> allSchemas = lookupMap.getLookupTable(false).values();
        JsonDocument jsonDocument = new JsonDocument();
        List<JsonDocument> schemas = new ArrayList<>();
        jsonDocument.put("schemas", schemas);
        for (NexusSchemaReference schemaReference : allSchemas) {
            try {

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
