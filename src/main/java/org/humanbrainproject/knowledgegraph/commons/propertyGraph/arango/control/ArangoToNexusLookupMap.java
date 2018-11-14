package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.statistics.boundary.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class ArangoToNexusLookupMap implements InitializingBean {

    @Autowired
    Structure structure;


    protected Logger logger = LoggerFactory.getLogger(ArangoToNexusLookupMap.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        refetch();
    }

    private static final Map<String, NexusSchemaReference> schemaReferenceMap = Collections.synchronizedMap(new HashMap<>());

    public static void addToSchemaReferenceMap(ArangoCollectionReference arangoName, NexusSchemaReference schemaReference){
        if(!schemaReferenceMap.containsKey(arangoName.getName())) {
            schemaReferenceMap.put(arangoName.getName(), schemaReference);
        }
    }

    public synchronized NexusSchemaReference getNexusSchema(ArangoCollectionReference arangoCollectionReference){
        NexusSchemaReference schemaReference = schemaReferenceMap.get(arangoCollectionReference.getName());
        if(schemaReference==null){
            //Refetch schemas from nexus and translate them
            refetch();
            schemaReference = schemaReferenceMap.get(arangoCollectionReference.getName());
        }
        return schemaReference;
    }

    private void refetch(){
        logger.info("Start fetching schemas - cache population in process");
        structure.getAllSchemasInMainSpace().forEach(ArangoCollectionReference::fromNexusSchemaReference);
        logger.info("Done fetching schemas - the cache is populated");
    }

    public Map<String, NexusSchemaReference> getLookupTable(boolean refetch){
        if(schemaReferenceMap.isEmpty() || refetch){
            refetch();
        }
        return schemaReferenceMap;
    }



}
