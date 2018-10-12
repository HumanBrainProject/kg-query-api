package org.humanbrainproject.knowledgegraph.control.arango.query;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.query.QueryParameters;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.HashSet;
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
        query.arangoDriver = Mockito.mock(ArangoDriver.class);
        Mockito.doReturn(gson.fromJson(collectionLabels, Set.class)).when(query.arangoDriver).getCollectionLabels();
        query.namingConvention = new ArangoNamingConvention();
        this.whitelistedOrganizations = new LinkedHashSet<>();
        this.whitelistedOrganizations.add("minds");
        this.whitelistedOrganizations.add("cscs");
        this.whitelistedOrganizations.add("neuralactivity");

    }


    @Test
    public void createMetaQuery() throws JSONException {
        //testSpecification.setSpecificationId("sample");
        ArangoMetaQueryBuilder queryBuilder = new ArangoMetaQueryBuilder(testSpecification);
        String query = this.query.createQuery(queryBuilder, new QueryParameters());

        System.out.println(query);
    }
}