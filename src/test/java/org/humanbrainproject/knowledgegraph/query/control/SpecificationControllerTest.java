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

package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.SpecificationQuery;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.query.entity.Filter;
import org.humanbrainproject.knowledgegraph.query.entity.Query;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.*;

public class SpecificationControllerTest {

    SpecificationController specificationController;

    @Before
    public void setup(){
        this.specificationController = new SpecificationController();
        this.specificationController.authorizationContext = Mockito.mock(AuthorizationContext.class);
        this.specificationController.queryContext = Mockito.mock(QueryContext.class);
        this.specificationController.specificationQuery = Mockito.mock(SpecificationQuery.class);
        this.specificationController.configuration = TestObjectFactory.createNexusConfiguration();
        this.specificationController.repository = Mockito.mock(ArangoRepository.class);
    }


    @Test(expected = RuntimeException.class)
    public void reflectSpecificationWithMultipleResponses() throws JSONException {
        Map fakeResult = Mockito.mock(Map.class);
        Query query = new Query("foo", TestObjectFactory.fooInstanceReference().getNexusSchema(), "fooVocab");
        Mockito.doReturn(Collections.singletonList(fakeResult)).when(this.specificationController.specificationQuery).queryForSimpleMap(Mockito.any());
        this.specificationController.releaseTreeBySpecification(Mockito.mock(Specification.class), query, null, TreeScope.ALL);
    }


    @Test
    public void reflectSpecification() throws JSONException {
        Map fakeResult = Mockito.mock(Map.class);
        Query query = new Query("foo", TestObjectFactory.fooInstanceReference().getNexusSchema(), "fooVocab");
        Mockito.doReturn(Collections.singletonList(fakeResult)).when(this.specificationController.specificationQuery).queryForSimpleMap(Mockito.any());
        Specification mock = Mockito.mock(Specification.class);
        Mockito.doReturn(TestObjectFactory.fooInstanceReference().getNexusSchema().getRelativeUrl().getUrl()).when(mock).getRootSchema();
        Map map = this.specificationController.releaseTreeBySpecification(mock, query, TestObjectFactory.fooInstanceReference(), TreeScope.ALL);
        Assert.assertEquals(fakeResult, map);
    }


    @Test
    public void queryForSpecification() throws IOException, SolrServerException {
        QueryResult fakeResult = Mockito.mock(QueryResult.class);
        Mockito.doReturn(fakeResult).when(this.specificationController.specificationQuery).queryForData(Mockito.any(), Mockito.any(), Mockito.any());
        QueryResult<List<Map>> listQueryResult = this.specificationController.queryForSpecification(Mockito.mock(Specification.class), null, new Filter(), null);
        Assert.assertEquals(fakeResult, listQueryResult);
    }
}