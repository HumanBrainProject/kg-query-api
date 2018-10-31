package org.humanbrainproject.knowledgegraph.indexing;

import com.github.jsonldjava.core.JsonLdConsts;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.OidcClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusDocumentConverter;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.control.inference.InferenceController;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.TodoList;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.nexusExt.control.InstanceController;
import org.humanbrainproject.knowledgegraph.nexusExt.control.ReleaseController;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SpringBootTest
@RunWith(SpringRunner.class)
@Ignore("System test")
public class FullIndexingTest {

    @Autowired
    InstanceController nexusInstanceController;

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    ReleaseController releaseController;

    @Autowired
    NexusDocumentConverter nexusDocumentConverter;

    @Autowired
    OidcClient oidcClient;

    Map<String, Object> payload;
    NexusInstanceReference instance;

    private void nexusInsertionTrigger(TodoList<?> todoList) throws IOException {
        //Trigger indexing for nexus uploads
        for (InsertOrUpdateInPrimaryStoreTodoItem<?> insertOrUpdateInPrimaryStoreTodoItem : todoList.getInsertOrUpdateInPrimaryStoreTodoItems()) {
            MainVertex object = insertOrUpdateInPrimaryStoreTodoItem.getObject();
            IndexingMessage indexingMessage2 = new IndexingMessage(object.getInstanceReference(), nexusDocumentConverter.createJsonFromVertex(object.getInstanceReference(), object));
            graphIndexing.insert(indexingMessage2);
        }
    }


    @Test
    public void create() throws IOException {
        payload = new LinkedHashMap<>();
        payload.put(SchemaOrgVocabulary.NAMESPACE+"foo", "bar");
        instance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().getNexusSchema(), "helloWorld", payload,oidcClient.getAuthorizationToken());

        //This trigger is typically done by Nexus itself - we're simulating the behavior.
        IndexingMessage indexingMessage = new IndexingMessage(instance, new Gson().toJson(payload));
        TodoList<?> insert = graphIndexing.insert(indexingMessage);
        nexusInsertionTrigger(insert);
    }



    @Test
    public void createAndRelease() throws IOException {
        create();

        IndexingMessage releaseMessage = releaseController.release(instance, instance.getRevision(), oidcClient.getAuthorizationToken());

        //Again the simulation of the indexing trigger
        TodoList<?> insert = graphIndexing.insert(releaseMessage);

        nexusInsertionTrigger(insert);

    }


    @Test
    public void changeAfterRelease() throws IOException {
        createAndRelease();

        payload.put(SchemaOrgVocabulary.NAMESPACE+"foo", "foobar");
        nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().getNexusSchema(), "helloWorld", payload, oidcClient.getAuthorizationToken());

        NexusInstanceReference instance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().getNexusSchema(), "helloWorld", payload, oidcClient.getAuthorizationToken());

        IndexingMessage indexingMessage = new IndexingMessage(instance, new Gson().toJson(payload));
        graphIndexing.insert(indexingMessage);
    }

    @Test
    public void unrelease() throws IOException {
        createAndRelease();
        Set<NexusInstanceReference> unrelease = releaseController.unrelease(instance, oidcClient.getAuthorizationToken());
        for (NexusInstanceReference nexusInstanceReference : unrelease) {
            graphIndexing.delete(nexusInstanceReference);
        }
    }


    @Test
    public void createAndEdit() throws IOException {
        create();
        Map<String, Object> editorPayload = new LinkedHashMap<>();
        editorPayload.put(SchemaOrgVocabulary.NAMESPACE+"foo", "editbar");
        editorPayload.put(SchemaOrgVocabulary.NAMESPACE+"bar", "editfoo");
        Map<String, String> reference = new LinkedHashMap<>();
        reference.put(JsonLdConsts.ID, configuration.getAbsoluteUrl(instance.getRelativeUrl()));
        editorPayload.put(InferenceController.ORIGINAL_PARENT_PROPERTY, reference);
        NexusInstanceReference editorInstance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().toSubSpace(SubSpace.EDITOR).getNexusSchema(), "helloWorld", editorPayload, oidcClient.getAuthorizationToken());

        IndexingMessage indexingMessage = new IndexingMessage(editorInstance, new Gson().toJson(editorPayload));
        TodoList<?> inserted = graphIndexing.insert(indexingMessage);
        nexusInsertionTrigger(inserted);

    }
}
