package org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.query;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.QueryParameters;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class ArangoSpecificationQueryTest {

    Specification testSpecification;
    ArangoSpecificationQuery query;
    Set<String> whitelistedOrganizations;

    @Before
    public void setup() throws IOException, JSONException {
        String specification = IOUtils.toString(this.getClass().getResourceAsStream("/apiSpec/sample.json"), "UTF-8");
        String collectionLabels = IOUtils.toString(this.getClass().getResourceAsStream("/collectionLabels.json"), "UTF-8");
        Gson gson = new Gson();
        this.testSpecification = new SpecificationInterpreter().readSpecification(specification);
        query = new ArangoSpecificationQuery();
        query.databaseFactory = Mockito.mock(ArangoDatabaseFactory.class);
        ArangoConnection mockConnection = Mockito.mock(ArangoConnection.class);
        Mockito.doReturn(mockConnection).when(query.databaseFactory).getDefaultDB();
        Mockito.doReturn(gson.fromJson(collectionLabels, Set.class).stream().map(c -> new ArangoCollectionReference(c.toString()))).when(mockConnection).getCollections();
        this.whitelistedOrganizations = new LinkedHashSet<>();
        this.whitelistedOrganizations.add("minds");
        this.whitelistedOrganizations.add("cscs");
        this.whitelistedOrganizations.add("neuralactivity");
    }



    @Test
    public void createMetaQuery() throws JSONException {
        //testSpecification.setSpecificationId("sample");
        ArangoMetaQueryBuilder queryBuilder = new ArangoMetaQueryBuilder(testSpecification);
        String query = this.query.createQuery(queryBuilder, new QueryParameters(null, null));
        System.out.println(query);
    }
}