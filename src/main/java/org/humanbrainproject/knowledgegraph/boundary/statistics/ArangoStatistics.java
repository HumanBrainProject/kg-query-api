package org.humanbrainproject.knowledgegraph.boundary.statistics;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.entity.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ArangoStatistics {

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoDriver arangoDriver;


    public Map<String, Object> getStructure() {
        ArangoDatabase db = arangoDriver.getOrCreateDB();
        Map<String, String> arangoNameMapping = repository.getArangoNameMapping(db);
        LinkedHashMap<String, Long> collectionCount = db.getCollections().parallelStream().filter(c -> c.getIsSystem() != null && !c.getIsSystem() && c.getType() == CollectionType.DOCUMENT).map(c -> repository.countInstances(c.getName(), arangoDriver)).sorted(Comparator.comparing(Tuple::getValue1)).collect(
                LinkedHashMap::new, (map, item) -> map.put(item.getValue1(), item.getValue2()), Map::putAll);
        Map<String, Object> map = new LinkedHashMap<>();
        for (String collectionName : collectionCount.keySet()) {
            Map<String, Object> propertiesWithCount = repository.getPropertyCount(collectionName, db);
            propertiesWithCount.put("totalCount", collectionCount.get(collectionName));
            String name = arangoNameMapping.get(collectionName);
            if (name != null) {
                map.put(name, propertiesWithCount);
            }
        }
        return map;
    }


}
