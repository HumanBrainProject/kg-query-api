package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Op;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Value;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SpecificationInterpreterTest {

    SpecificationInterpreter interpreter;
    String testSpecification;
    String filterSpecification;


    @Before
    public void setup() throws IOException{
        interpreter = new SpecificationInterpreter();
        testSpecification = IOUtils.toString(this.getClass().getResourceAsStream("/specification.json"), "UTF-8");
        filterSpecification = IOUtils.toString(this.getClass().getResourceAsStream("/filter_specification.json"), "UTF-8");
    }

    @Test
    public void readSpecification() throws JSONException {
        Specification specification = interpreter.readSpecification(testSpecification, null);
        assertEquals("https://nexus-dev.humanbrainproject.org/v0/schemas/minds/core/dataset/v1.0.0", specification.rootSchema);
        assertEquals(17, specification.fields.size());
    }

    @Test
    public void readFilterSpecification() throws JSONException {
        Specification specification = interpreter.readSpecification(filterSpecification, null);
        assertNotEquals(null, specification.fields.get(0).fieldFilter);
        assertEquals(Op.EQUALS.name(), specification.fields.get(0).fieldFilter.getOp().name());
        assertEquals(new Value("test"), specification.fields.get(0).fieldFilter.getExp());
    }
}