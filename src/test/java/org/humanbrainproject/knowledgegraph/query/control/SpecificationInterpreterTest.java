package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SpecificationInterpreterTest {

    SpecificationInterpreter interpreter;
    String testSpecification;


    @Before
    public void setup() throws IOException{
        interpreter = new SpecificationInterpreter();
        testSpecification = IOUtils.toString(this.getClass().getResourceAsStream("/specification.json"), "UTF-8");
    }

    @Test
    public void readSpecification() throws JSONException {
        Specification specification = interpreter.readSpecification(testSpecification, null);
        assertEquals("https://nexus-dev.humanbrainproject.org/v0/schemas/minds/core/dataset/v1.0.0", specification.rootSchema);
        assertEquals(17, specification.fields.size());
    }
}