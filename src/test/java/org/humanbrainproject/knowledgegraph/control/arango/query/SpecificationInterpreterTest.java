package org.humanbrainproject.knowledgegraph.control.arango.query;

import com.github.jsonldjava.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.query.QueryParameters;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

public class SpecificationInterpreterTest {

    SpecificationInterpreter interpreter;
    String testSpecification;
    ArangoSpecificationQuery query;



    @Before
    public void setup() throws IOException, JSONException {
        interpreter = new SpecificationInterpreter();
        String json = IOUtils.toString(this.getClass().getResourceAsStream("/specification.json"), "UTF-8");
        testSpecification = JsonUtils.toString(new JsonLdStandardization().fullyQualify(json));
        query = new ArangoSpecificationQuery();
        query.namingConvention = new ArangoNamingConvention();
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
        query.queryForSpecification(specification, Collections.EMPTY_SET, new QueryParameters(), null);
    }
}