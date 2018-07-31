package org.humanbrainproject.knowledgegraph.control.neo4j;

import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.commons.lang.NotImplementedException;
import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.humanbrainproject.knowledgegraph.control.VertexRepository;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Neo4jRepository extends VertexRepository<Transaction> {

    @Autowired
    Configuration configuration;

    @Override
    protected Integer getRevisionById(JsonLdVertex vertex, Transaction tx) {
        String id = vertex.getId();
        String type = vertex.getType();
        List<Record> results = tx.run(String.format("MATCH (n:`%s`) WHERE n.`%s`=$id RETURN n.`%s` as rev", type, JsonLdConsts.ID, configuration.getRev()), Values.parameters("id", id)).list();
        if (results == null || results.isEmpty()) {
            return null;
        }
        int maxRev = 0;
        for (Record result : results) {
            int rev = result.get("rev", -1);
            if (rev > maxRev) {
                maxRev = rev;
            }
        }
        return maxRev;
    }

    @Override
    public void deleteVertex(String entityName, String identifier,  Transaction tx) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("MATCH (n) OPTIONAL MATCH (n)-[r]-() WHERE n.`%s`=$id DELETE r, n", JsonLdConsts.ID));
        logger.info("Delete vertex");
        logger.debug(query.toString());
        tx.run(query.toString(), Values.parameters("id", identifier));
    }

    @Override
    protected void updateVertex(JsonLdVertex vertex, Transaction tx) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("MATCH (n) WHERE n.`%s`=$id\n", JsonLdConsts.ID));
        query.append("SET n = {");
        Map<String, Object> propertyMap = new HashMap<>();
        for (JsonLdProperty jsonLdProperty : vertex.getProperties()) {
            query.append(String.format("`%s`: $property%d", jsonLdProperty.getName(), propertyMap.size()));
            propertyMap.put(String.format("property%d", propertyMap.size()), jsonLdProperty.getValue());
            if (propertyMap.size() < vertex.getProperties().size()) {
                query.append(", ");
            }
        }
        query.append("}");
        propertyMap.put("id", vertex.getId());
        String q = query.toString();
        logger.info("Update vertex");
        logger.debug(q);
        tx.run(q, propertyMap);
    }

    @Override
    protected void insertVertex(JsonLdVertex vertex, Transaction tx) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("CREATE (e:`%s`) \n", vertex.getType()));
        Map<String, Object> propertyMap = new HashMap<>();
        boolean idDefined = false;
        boolean revisionDefined = false;
        for (JsonLdProperty jsonLdProperty : vertex.getProperties()) {
            int propertyIndex = propertyMap.size();
            query.append(String.format("SET e.`%s` = $propertyValue%d\n", jsonLdProperty.getName(), propertyIndex));
            propertyMap.put(String.format("propertyValue%d", propertyIndex), jsonLdProperty.getValue());
            if (jsonLdProperty.getName().equals(JsonLdConsts.ID)) {
                idDefined = true;
            }
            if (jsonLdProperty.getName().equals(configuration.getRev())) {
                revisionDefined = true;
            }
        }
        if (!idDefined) {
            query.append(String.format("SET e.`%s` = $propertyValue%d\n", JsonLdConsts.ID, propertyMap.size()));
            propertyMap.put(String.format("propertyValue%d", propertyMap.size()), vertex.getId());
        }
        if (!revisionDefined) {
            query.append(String.format("SET e.`%s` = $propertyValue%d\n", configuration.getRev(), propertyMap.size()));
            propertyMap.put(String.format("propertyValue%d", propertyMap.size()), vertex.getRevision());
        }
        String q = query.toString();
        logger.info(String.format("Insert vertex %s", vertex.getId()));
        logger.debug(q);
        tx.run(q, propertyMap).list();
    }

    @Override
    protected void createEdge(JsonLdVertex vertex, JsonLdEdge edge, int orderNumber, Transaction tx) {
        String conn = String.format("MATCH p=(o)-[r]->(t) WHERE o.`%s`=$originId AND t.`%s`=$targetId RETURN p", JsonLdConsts.ID, JsonLdConsts.ID);
        List<Record> results = tx.run(conn, Values.parameters("originId", vertex.getId(), "targetId", edge.getReference())).list();
        if (results != null && !results.isEmpty()) {
            logger.info("Relation already exists - check for update");
        } else {
            String findTarget = String.format("MATCH (target) WHERE target.`%s`=$targetId RETURN target", JsonLdConsts.ID);
            if (!tx.run(findTarget, Values.parameters("targetId", edge.getReference())).hasNext()) {
                logger.info("Relation does not exists - but can not find related element. Create placeholder");
                String createQuery = String.format("CREATE (e:`%s`) SET e.`%s`=$id", UNRESOLVED_LINKS, JsonLdConsts.ID);
                tx.run(createQuery, Values.parameters("id", edge.getReference())).consume();
            } else {
                logger.info("Relation does not exists - but target does. We create the edge");
            }
            StringBuilder query = new StringBuilder();
            query.append(String.format("MATCH (origin) WHERE origin.`%s`=$originId\n MATCH (target) WHERE target.`%s`=$targetId\n", JsonLdConsts.ID, JsonLdConsts.ID));
            query.append(String.format("CREATE (origin)-[r:`%s`]->(target)", edge.getName()));
            tx.run(query.toString(), Values.parameters("originId", vertex.getId(), "targetId", edge.getReference()));
        }
    }


    @Override
    protected boolean hasEdge(JsonLdVertex vertex, JsonLdEdge edge, Transaction transactionOrConnection) throws JSONException {
        throw new NotImplementedException();
    }

    @Override
    protected void updateEdge(JsonLdVertex vertex, JsonLdEdge edge, int orderNumber, Transaction transactionOrConnection) throws JSONException {
        throw new NotImplementedException();
    }

    @Override
    public void updateUnresolved(JsonLdVertex vertex, Transaction tx) {
        String query = String.format("MATCH (n:`http://schema.hbp.eu/propertygraph/unresolved`) MATCH (n)<-[r]-(s) MATCH (s)-[r2]->(x) WHERE n.`%s`=$id AND x.`%s`=$id AND NOT x:`http://schema.hbp.eu/propertygraph/unresolved` RETURN n, r, s", JsonLdConsts.ID, JsonLdConsts.ID);
        StatementResult result = tx.run(query, Values.parameters("id", vertex.getId()));
        for (Record record : result.list()) {
            System.out.println(record);
        }
    }

}
