package org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.query;

import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.QueryParameters;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SpecificationInterpreterTest {

    SpecificationInterpreter interpreter;
    String testSpecification;
    ArangoSpecificationQuery query;



    @Before
    public void setup() throws IOException, JSONException {
        interpreter = new SpecificationInterpreter();
        testSpecification = IOUtils.toString(this.getClass().getResourceAsStream("/specification.json"), "UTF-8");
        query = new ArangoSpecificationQuery();
        query.configuration = new NexusConfiguration();
        query.databaseFactory = Mockito.mock(ArangoDatabaseFactory.class);
    }

    @Test
    public void readSpecification() throws JSONException {
        Specification specification = interpreter.readSpecification(testSpecification);
        assertEquals("https://nexus-dev.humanbrainproject.org/v0/schemas/minds/core/dataset/v0.0.4", specification.rootSchema);
        assertEquals(17, specification.fields.size());
    }

    @Test
    @Ignore("This test requires a backend and is for manual testing only")
    public void readSpecificationAndCreateQuery() throws JSONException {
        Specification specification = interpreter.readSpecification(testSpecification);
        query.queryForSpecification(specification, Collections.EMPTY_SET, new QueryParameters(null, null), null);
    }
}