package org.humanbrainproject.knowledgegraph.structure.boundary;

import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.SystemNexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoToNexusLookupMap;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Structure {

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    SystemNexusClient systemNexusClient;

    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    @Autowired
    ArangoToNexusLookupMap lookupMap;


    public Set<NexusSchemaReference> getAllSchemasInMainSpace(){
        Set<NexusSchemaReference> allSchemas = systemNexusClient.getAllSchemas(null, null);
        return allSchemas.stream().map(s -> s.toSubSpace(SubSpace.MAIN)).collect(Collectors.toSet());
    }


    public JsonDocument getStructureForSchema(NexusSchemaReference schemaReference){
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.put("id", schemaReference.getRelativeUrl().getUrl());
        jsonDocument.put("group", schemaReference.getOrganization());
        jsonDocument.put("label", semanticsToHumanTranslator.translateNexusSchemaReference(schemaReference));
        //TODO reflect on schema
        List<Map> attributesWithCount = repository.getAttributesWithCount(ArangoCollectionReference.fromNexusSchemaReference(schemaReference));
        attributesWithCount.forEach(map -> {
            Object attribute = map.get("attribute");
            if(attribute!=null){
                map.put("label", semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel(attribute.toString()));
            }

        });

        jsonDocument.put("properties", attributesWithCount);
        return jsonDocument;
    }

    public JsonDocument getStructure() {
        Collection<NexusSchemaReference> allSchemas = lookupMap.getLookupTable(false).values();
        JsonDocument jsonDocument = new JsonDocument();
        List<JsonDocument> schemas = new ArrayList<>();
        jsonDocument.put("schemas", schemas);
        for (NexusSchemaReference schemaReference : allSchemas) {
            schemas.add(getStructureForSchema(schemaReference));
        }
        return jsonDocument;
    }

    public void reflectOnSpecifications(){
        List<Map> internalDocuments = repository.getInternalDocuments(ArangoQuery.SPECIFICATION_QUERIES);
        System.out.println(internalDocuments);
    }




}
