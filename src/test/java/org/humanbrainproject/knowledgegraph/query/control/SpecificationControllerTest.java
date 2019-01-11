package org.humanbrainproject.knowledgegraph.query.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.DefaultAuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.SpecificationQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
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

import java.util.*;

public class SpecificationControllerTest {

    SpecificationController specificationController;

    @Before
    public void setup(){
        this.specificationController = new SpecificationController();
        this.specificationController.authorizationContext = Mockito.mock(DefaultAuthorizationContext.class);
        this.specificationController.queryContext = Mockito.mock(QueryContext.class);
        this.specificationController.specificationQuery = Mockito.mock(SpecificationQuery.class);
        this.specificationController.configuration = TestObjectFactory.createNexusConfiguration();
        this.specificationController.repository = Mockito.mock(ArangoRepository.class);
    }


    @Test
    public void reflectSpecificationWithSingleResponse() throws JSONException {
        QueryResult fakeResult = Mockito.mock(QueryResult.class);
        Mockito.doReturn(Collections.singletonList(new HashMap<>())).when(fakeResult).getResults();
        Query query = new Query("foo", TestObjectFactory.fooInstanceReference().getNexusSchema(), "fooVocab");
        Mockito.doReturn(fakeResult).when(this.specificationController.specificationQuery).query(Mockito.any());

        Map map = this.specificationController.reflectSpecification(Mockito.mock(Specification.class), query);
        Assert.assertTrue(map.isEmpty());
    }


    @Test(expected = RuntimeException.class)
    public void reflectSpecificationWithMultipleResponses() throws JSONException {
        QueryResult fakeResult = Mockito.mock(QueryResult.class);
        Mockito.doReturn(Arrays.asList(new HashMap<>(), new HashMap<>())).when(fakeResult).getResults();
        Query query = new Query("foo", TestObjectFactory.fooInstanceReference().getNexusSchema(), "fooVocab");
        Mockito.doReturn(fakeResult).when(this.specificationController.specificationQuery).query(Mockito.any());
        this.specificationController.reflectSpecification(Mockito.mock(Specification.class), query);
    }


    @Test
    public void reflectSpecification() throws JSONException {
        QueryResult fakeResult = Mockito.mock(QueryResult.class);
        Query query = new Query("foo", TestObjectFactory.fooInstanceReference().getNexusSchema(), "fooVocab");
        Mockito.doReturn(fakeResult).when(this.specificationController.specificationQuery).query(Mockito.any());

        Map map = this.specificationController.reflectSpecification(Mockito.mock(Specification.class), query);
        Assert.assertNull(map);
    }


    @Test
    public void queryForSpecificationWithDocumentReferences() throws JSONException {
        QueryResult fakeResult = Mockito.mock(QueryResult.class);
        Mockito.doReturn(fakeResult).when(this.specificationController.specificationQuery).query(Mockito.any());
        QueryResult<List<Map>> listQueryResult = this.specificationController.queryForSpecification(Mockito.mock(Specification.class), Collections.singleton(new ArangoDocumentReference(new ArangoCollectionReference("foo"), "bar")), null, new Filter());
        Assert.assertEquals(fakeResult, listQueryResult);
    }

    @Test
    public void queryForSpecificationWithEmptyDocumentReferences() throws JSONException {
        QueryResult<List<Map>> listQueryResult = this.specificationController.queryForSpecification(Mockito.mock(Specification.class), Collections.emptySet(), null, null);

        Assert.assertTrue(listQueryResult.getSize()==0);
        Assert.assertTrue(listQueryResult.getTotal()==0);
        Assert.assertTrue(listQueryResult.getResults().isEmpty());
    }
}