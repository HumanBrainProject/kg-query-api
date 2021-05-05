/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.commons.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.junit.Assert;
import org.junit.Test;

public class RestUtilsTest {

    @Test
    public void splitCommaSeparatedValues() {
        String[] strings = RestUtils.splitCommaSeparatedValues("foo, bar");
        Assert.assertEquals(2, strings.length);
        Assert.assertEquals("foo", strings[0]);
        Assert.assertEquals("bar", strings[1]);
    }

    @Test
    public void splitCommaSeparatedValuesNull() {
        String[] strings = RestUtils.splitCommaSeparatedValues(null);
        Assert.assertNull(strings);
    }

    @Test
    public void toJsonResultIfPossible() {
        QueryResult<String> stringResult = new QueryResult<>();
        stringResult.setResults("{\"foo\":\"bar\"}");

        QueryResult queryResult = RestUtils.toJsonResultIfPossible(stringResult);
        Assert.assertTrue(queryResult.getResults() instanceof ObjectNode);
        ObjectNode node = (ObjectNode)queryResult.getResults();
        Assert.assertEquals("bar", node.get("foo").textValue());

    }

    @Test
    public void toJsonResultIfPossibleNoString() {
        QueryResult<Integer> integerResult = new QueryResult<>();
        integerResult.setResults(10);

        QueryResult queryResult = RestUtils.toJsonResultIfPossible(integerResult);
        Assert.assertEquals(10, queryResult.getResults());
    }

    @Test
    public void toJsonResultIfPossibleNull() {
        QueryResult queryResult = RestUtils.toJsonResultIfPossible(null);
        Assert.assertNull(queryResult);

    }
}