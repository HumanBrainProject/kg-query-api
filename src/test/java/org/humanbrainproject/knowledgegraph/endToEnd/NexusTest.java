package org.humanbrainproject.knowledgegraph.endToEnd;

import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseTransaction;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.solr.Solr;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoItemWithDatabaseConnection;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class NexusTest {

    @Autowired
    NexusClient nexusClient;

    @Autowired
    DatabaseTransaction databaseTransaction;

    @Autowired
    Solr solr;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Before
    public void setup(){
        Mockito.reset(solr);
        Mockito.reset(databaseTransaction);
        Mockito.reset(databaseTransaction);
    }

    @Test
    public void insertNewDocument(){
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.addType("foo");
        jsonDocument.addToProperty("foo", "bar");
        Mockito.doAnswer(invocationOnMock -> {
            TodoList todoList = invocationOnMock.getArgument(0);
            assertInsertionOfNonExistingInstance(todoList);
            return null;
        }).when(databaseTransaction).execute(Mockito.any(TodoList.class));

        nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, "test/core/foo/v1.0.0/foobar"), 1, jsonDocument, new InternalMasterKey());
    }


    @Test
    public void insertSpatialAnchoringDocument() throws IOException, SolrServerException {

        NexusInstanceReference referencedInstance = new NexusInstanceReference("foo", "core", "bar", "v1.0.0", "foobar");
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.addType(HBPVocabulary.SPATIAL_TYPE);
        jsonDocument.addToProperty(HBPVocabulary.SPATIAL_COORDINATES, "116.76450275662296, 420.38180695602125,371.05990195986874,32.25523977201931,-483.5743708352436,-69.49201572740994, 88.9241921312597,51.94987159616494,-320.22912581890387");
        jsonDocument.addToProperty(HBPVocabulary.SPATIAL_REFERENCESPACE, "refSpace");
        jsonDocument.addReference(HBPVocabulary.SPATIAL_LOCATED_INSTANCE, nexusConfiguration.getAbsoluteUrl(referencedInstance));

        //Assure that solr registration is invoked with appropriate parameters
        Mockito.doAnswer(invocationOnMock -> {
            String id = invocationOnMock.getArgument(0);
            String referenceSpace = invocationOnMock.getArgument(1);
            Collection<ThreeDVector> vectors = invocationOnMock.getArgument(2);
            assertEquals(ArangoDocumentReference.fromNexusInstance(referencedInstance).getId(), id);
            assertEquals("refSpace", referenceSpace);
            assertFalse(vectors.isEmpty());
            return null;
        }).when(solr).registerPoints(Mockito.anyString(), Mockito.anyString(), Mockito.anyCollection());

        Mockito.doAnswer(invocationOnMock -> {
            //There's no additional action in arango - therefore, we have the same structure of the TodoList as with a regular insert/update
            TodoList todoList = invocationOnMock.getArgument(0);
            assertInsertionOfNonExistingInstance(todoList);
            return null;
        }).when(databaseTransaction).execute(Mockito.any(TodoList.class));

        nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, "test/core/foo/v1.0.0/foobar"), 1, jsonDocument, new InternalMasterKey());

        //Assure that solr indexing is triggered
        Mockito.verify(solr).registerPoints(Mockito.anyString(), Mockito.anyString(), Mockito.anyCollection());
        //Assure that the databaseTransaction execution is triggered
        Mockito.verify(databaseTransaction).execute(Mockito.any(TodoList.class));
    }



    @Test
    public void insertLinkingInstance() {
        NexusInstanceReference fromInstance = new NexusInstanceReference("foo", "core", "bar", "v1.0.0", "foobar");
        NexusInstanceReference toInstance = new NexusInstanceReference("bar", "core", "foo", "v1.0.0", "barfoo");
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.addType(HBPVocabulary.LINKING_INSTANCE_TYPE);
        jsonDocument.addReference(HBPVocabulary.LINKING_INSTANCE_FROM, nexusConfiguration.getAbsoluteUrl(fromInstance));
        jsonDocument.addReference(HBPVocabulary.LINKING_INSTANCE_TO, nexusConfiguration.getAbsoluteUrl(toInstance));

        Mockito.doAnswer(invocationOnMock -> {
            //There's no additional action in arango - therefore, we have the same structure of the TodoList as with a regular insert/update
            TodoList todoList = invocationOnMock.getArgument(0);
            assertNumberOfItems(todoList.getInsertTodoItems(), "kg", 1);
            assertNumberOfItems(todoList.getInsertTodoItems(), "kg_inferred", 1);
            Map<String, List<InsertTodoItem>> byDatabase = groupByDatabase(todoList.getInsertTodoItems());
            List<InsertTodoItem> kg = byDatabase.get("kg");
            assertEquals(2, kg.get(0).getVertex().getEdges().size());

            List<InsertTodoItem> kgInferred = byDatabase.get("kg_inferred");
            assertEquals(2, kgInferred.get(0).getVertex().getEdges().size());
            return null;
        }).when(databaseTransaction).execute(Mockito.any(TodoList.class));

        nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, "test/core/foo/v1.0.0/foobar"), 1, jsonDocument, new InternalMasterKey());

        //Assure that the databaseTransaction execution is triggered
        Mockito.verify(databaseTransaction).execute(Mockito.any(TodoList.class));
    }

    @Test
    public void updateNonExistingDocument(){
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.addType("foo");
        jsonDocument.addToProperty("foo", "bar");
        Mockito.doAnswer(invocationOnMock -> {
            TodoList todoList = invocationOnMock.getArgument(0);
            assertInsertionOfNonExistingInstance(todoList);
            return null;
        }).when(databaseTransaction).execute(Mockito.any(TodoList.class));

        nexusClient.put(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, "test/core/foo/v1.0.0/foobar"), 2, jsonDocument, new InternalMasterKey());
    }


    @Test
    public void release(){


    }


    @Test
    public void reRelease(){

    }

    @Test
    public void unrelease(){

    }

    @Test
    public void reconcile(){
        //create original instance
        NexusInstanceReference originalInstance = new NexusInstanceReference("foo", "core", "bar", "v1.0.0", "foobar");
        JsonDocument originalDocument = new JsonDocument();
        originalDocument.addType("foo");
        originalDocument.addToProperty("foo", "bar");
        nexusClient.post(originalInstance.getRelativeUrl(), 1, originalDocument, new InternalMasterKey());

        //create extension instance
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.addType("foo");
        jsonDocument.addToProperty("bar", "foo");
        String absoluteUrlOfOriginalInstance = nexusConfiguration.getAbsoluteUrl(originalInstance);
        jsonDocument.addReference(HBPVocabulary.INFERENCE_EXTENDS, absoluteUrlOfOriginalInstance);

        Mockito.doAnswer(invocationOnMock -> {
            TodoList todoList = invocationOnMock.getArgument(0);

            //This is a reconciled instance - we therefore expect one regular insert (in "kg") and one insertion into the primary store (materialization of the inference).

            assertNumberOfItems(todoList.getInsertTodoItems(), "kg", 1);
            assertNumberOfItems(todoList.getInsertTodoItems(), "kg_inferred", 0);

            assertEquals(1, todoList.getInsertOrUpdateInPrimaryStoreTodoItems().size());
            InsertOrUpdateInPrimaryStoreTodoItem insertOrUpdateInPrimaryStoreTodoItem = todoList.getInsertOrUpdateInPrimaryStoreTodoItems().get(0);
            SubSpace subSpace = insertOrUpdateInPrimaryStoreTodoItem.getVertex().getInstanceReference().getNexusSchema().getSubSpace();
            assertEquals(SubSpace.INFERRED, subSpace);

            String inferenceOfId = (String)((Map)insertOrUpdateInPrimaryStoreTodoItem.getVertex().getQualifiedIndexingMessage().getQualifiedMap().get(HBPVocabulary.INFERENCE_OF)).get(JsonLdConsts.ID);
            assertEquals(absoluteUrlOfOriginalInstance, inferenceOfId);



            return null;
        }).when(databaseTransaction).execute(Mockito.any(TodoList.class));

        nexusClient.post(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, "test"+ SubSpace.EDITOR.getPostFix()+"/core/foo/v1.0.0/barfoo"), 1, jsonDocument, new InternalMasterKey());
    }


    @Test
    public void deleteDocument(){
        Mockito.doAnswer(invocationOnMock -> {
            TodoList todoList = invocationOnMock.getArgument(0);
            assertTrue(todoList.getInsertOrUpdateInPrimaryStoreTodoItems().isEmpty());
            assertTrue(todoList.getInsertTodoItems().isEmpty());
            assertNumberOfItems(todoList.getDeleteTodoItems(), "kg", 1);
            assertNumberOfItems(todoList.getDeleteTodoItems(), "kg_inferred", 1);
            assertNumberOfItems(todoList.getDeleteTodoItems(), "kg_released", 1);
            return null;
        }).when(databaseTransaction).execute(Mockito.any(TodoList.class));

        nexusClient.delete(new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, "test/core/foo/v1.0.0/foobar"), 2, new InternalMasterKey());
    }

    private void assertHasBlacklistItems(List<InsertTodoItem> todoItems, String database, boolean hasBlacklistItems){
        Map<String, List<InsertTodoItem>> byDatabase = groupByDatabase(todoItems);
        List<InsertTodoItem> list = byDatabase.get(database);
        if(list!=null){
            list.forEach( e->  assertTrue(e.getBlacklist().isEmpty() != hasBlacklistItems));
        }
        else{
            Assert.assertFalse(hasBlacklistItems);
        }
    }


    private void assertNumberOfItems(List<? extends TodoItemWithDatabaseConnection> todoItems, String database, int numberOfOccurences){
        Map<String, ? extends List<? extends TodoItemWithDatabaseConnection>> byDatabase = groupByDatabase(todoItems);
        List<? extends TodoItemWithDatabaseConnection> databaseEntries = byDatabase.get(database);
        assertEquals(numberOfOccurences, databaseEntries==null ? 0 : databaseEntries.size());
    }

    private <T extends TodoItemWithDatabaseConnection> Map<String, List<T>> groupByDatabase(List<T> todoItems){
        return todoItems.stream().collect(Collectors.groupingBy(f -> f.getDatabaseConnection(ArangoConnection.class).getDatabaseName()));
    }


    private void assertInsertionOfNonExistingInstance(TodoList todoList) {
        assertTrue(todoList.getDeleteTodoItems().isEmpty());
        assertTrue(todoList.getInsertOrUpdateInPrimaryStoreTodoItems().isEmpty());
        assertEquals(2, todoList.getInsertTodoItems().size());

        assertNumberOfItems(todoList.getInsertTodoItems(), "kg", 1);
        assertHasBlacklistItems(todoList.getInsertTodoItems(), "kg", false);

        assertNumberOfItems(todoList.getInsertTodoItems(), "kg_inferred", 1);
        //The inferred space contains - other than the default db - blacklisted properties.
        assertHasBlacklistItems(todoList.getInsertTodoItems(), "kg_inferred", true);
    }

}