package org.humanbrainproject.knowledgegraph.statistics.boundary;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Tuple;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Statistics {

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;


    public Map<String, Object> getStructure() {
        ArangoDatabase db = databaseFactory.getDefaultDB().getOrCreateDB();
        Map<String, String> arangoNameMapping = repository.getArangoNameMapping(db);
        LinkedHashMap<ArangoCollectionReference, Long> collectionCount = db.getCollections().parallelStream().filter(c -> c.getIsSystem() != null && !c.getIsSystem() && c.getType() == CollectionType.DOCUMENT).map(c -> repository.countInstances(new ArangoCollectionReference(c.getName()), databaseFactory.getDefaultDB())).sorted(Comparator.comparing(Tuple::getValue1)).collect(
                LinkedHashMap::new, (map, item) -> map.put(new ArangoCollectionReference(item.getValue1()), item.getValue2()), Map::putAll);
        Map<String, Object> map = new LinkedHashMap<>();
        for (ArangoCollectionReference collection : collectionCount.keySet()) {
            Map<String, Object> propertiesWithCount = repository.getPropertyCount(collection, db);
            propertiesWithCount.put("totalCount", collectionCount.get(collection));
            String name = arangoNameMapping.get(collection);
            if (name != null) {
                map.put(name, propertiesWithCount);
            }
        }
        return map;
    }


}
