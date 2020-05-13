/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        Specification specification = interpreter.readSpecification(testSpecification, null, null);
        assertEquals("https://nexus-dev.humanbrainproject.org/v0/schemas/minds/core/dataset/v1.0.0", specification.getRootSchema());
        assertEquals(17, specification.getFields().size());
    }

    @Test
    public void readFilterSpecification() throws JSONException {
        Specification specification = interpreter.readSpecification(filterSpecification, null, null);
        assertNotEquals(null, specification.getFields().get(0).fieldFilter);
        assertEquals(Op.EQUALS.name(), specification.getFields().get(0).fieldFilter.getOp().name());
        assertEquals(new Value("test"), specification.getFields().get(0).fieldFilter.getValue());
    }
}