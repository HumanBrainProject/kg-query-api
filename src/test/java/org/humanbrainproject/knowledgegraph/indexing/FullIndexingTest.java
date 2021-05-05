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

package org.humanbrainproject.knowledgegraph.indexing;

import com.github.jsonldjava.core.JsonLdConsts;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.InsertOrUpdateInPrimaryStoreTodoItem;
import org.humanbrainproject.knowledgegraph.indexing.entity.todo.TodoList;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.releasing.control.ReleaseControl;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
@Ignore("Integration test")
public class FullIndexingTest {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    InstanceManipulationController nexusInstanceController;

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    ReleaseControl releaseControl;

    @Autowired
    JsonTransformer jsonTransformer;

    JsonDocument payload;

    NexusInstanceReference instance;


    @Before
    public void setup(){
        authorizationContext.setMasterCredential();
    }

    private void nexusInsertionTrigger(TodoList todoList) {
        //Trigger indexing for nexus uploads
        for (InsertOrUpdateInPrimaryStoreTodoItem insertOrUpdateInPrimaryStoreTodoItem : todoList.getInsertOrUpdateInPrimaryStoreTodoItems()) {
            Vertex object = insertOrUpdateInPrimaryStoreTodoItem.getVertex();
            IndexingMessage indexingMessage2 = new IndexingMessage(object.getInstanceReference(), jsonTransformer.getMapAsJson(object.getQualifiedIndexingMessage().getQualifiedMap()), "2018-10-31", "Foo");
            graphIndexing.insert(indexingMessage2);
        }
    }


    @Test
    public void create() {
        payload = new JsonDocument();
        payload.put(SchemaOrgVocabulary.NAMESPACE+"foo", "bar");
        instance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().getNexusSchema(), "helloWorldNoEdit3", payload,null);

        //This trigger is typically done by Nexus itself - we're simulating the behavior.
        IndexingMessage indexingMessage = new IndexingMessage(instance, new Gson().toJson(payload), "2018-10-31", "Foo");
        TodoList insert = graphIndexing.insert(indexingMessage);
        nexusInsertionTrigger(insert);
    }


    @Test
    public void createWithEditor() {
        JsonDocument document =  new JsonDocument();
        document.put(SchemaOrgVocabulary.NAMESPACE+"foo", "bar");
        instance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooEditorInstanceReference().getNexusSchema(), "helloWorldFromEditor3", document, null);

        //This trigger is typically done by Nexus itself - we're simulating the behavior.
        IndexingMessage indexingMessage = new IndexingMessage(instance, new Gson().toJson(document), "2018-10-31", "Foo");
        TodoList insert = graphIndexing.insert(indexingMessage);
        nexusInsertionTrigger(insert);
    }


    @Test
    public void createWithEditorAndRelation() {
        createWithEditor();

        JsonDocument document = new JsonDocument();
        document.addReference(SchemaOrgVocabulary.NAMESPACE+"foolink", configuration.getAbsoluteUrl(instance));

        NexusInstanceReference helloWorldFromEditorWithLink = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooEditorInstanceReference().getNexusSchema(), "helloWorldFromEditorWithLink2", document,null);

        //This trigger is typically done by Nexus itself - we're simulating the behavior.
        IndexingMessage indexingMessage = new IndexingMessage(helloWorldFromEditorWithLink, new Gson().toJson(document), "2018-10-31", "Foo");
        TodoList insert = graphIndexing.insert(indexingMessage);
        nexusInsertionTrigger(insert);
    }




    @Test
    public void createAndRelease() throws IOException {
        create();

        IndexingMessage releaseMessage = releaseControl.release(instance, instance.getRevision());

        //Again the simulation of the indexing trigger
        TodoList insert = graphIndexing.insert(releaseMessage);

        nexusInsertionTrigger(insert);

    }


    @Test
    public void changeAfterRelease() throws IOException {
        createAndRelease();

        payload.put(SchemaOrgVocabulary.NAMESPACE+"foo", "foobar");
        payload.put(SchemaOrgVocabulary.IDENTIFIER, null);
        nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().getNexusSchema(), "helloWorld2", payload,null);

        NexusInstanceReference instance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().getNexusSchema(), "helloWorld2", payload, null);

        IndexingMessage indexingMessage = new IndexingMessage(instance, new Gson().toJson(payload), "2018-10-31", "Foo");
        graphIndexing.insert(indexingMessage);
    }

    @Test
    public void unrelease() throws IOException {
        createAndRelease();
        NexusInstanceReference unreleased = releaseControl.unrelease(instance);
        graphIndexing.delete(unreleased);
    }


    @Test
    public void createAndEdit(){
        create();
        JsonDocument editorPayload= new JsonDocument();
        editorPayload.put(SchemaOrgVocabulary.NAMESPACE+"foo", "editbar");
        editorPayload.put(SchemaOrgVocabulary.NAMESPACE+"bar", "editfoo");
        Map<String, String> reference = new LinkedHashMap<>();
        reference.put(JsonLdConsts.ID, configuration.getAbsoluteUrl(instance));
        editorPayload.put(HBPVocabulary.INFERENCE_EXTENDS, reference);
        NexusInstanceReference editorInstance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().toSubSpace(SubSpace.EDITOR).getNexusSchema(), "helloWorld2", editorPayload, null);

        IndexingMessage indexingMessage = new IndexingMessage(editorInstance, new Gson().toJson(editorPayload), "2018-10-31", "Foo");
        TodoList inserted = graphIndexing.insert(indexingMessage);
        nexusInsertionTrigger(inserted);

    }

    @Test
    public void createEditReleaseInferred() {
        create();
        JsonDocument editorPayload= new JsonDocument();
        editorPayload.put(SchemaOrgVocabulary.NAMESPACE+"foo", "editbar");
        editorPayload.put(SchemaOrgVocabulary.NAMESPACE+"bar", "editfoo");
        Map<String, String> reference = new LinkedHashMap<>();
        reference.put(JsonLdConsts.ID, configuration.getAbsoluteUrl(instance));
        editorPayload.put(HBPVocabulary.INFERENCE_EXTENDS, reference);
        NexusInstanceReference editorInstance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().toSubSpace(SubSpace.EDITOR).getNexusSchema(), "helloWorld2", editorPayload,null);

        IndexingMessage indexingMessage = new IndexingMessage(editorInstance, new Gson().toJson(editorPayload), "2018-10-31", "Foo");
        TodoList inserted = graphIndexing.insert(indexingMessage);
        nexusInsertionTrigger(inserted);

    }

    @Test
    public void createLinkingInstance() {
        create();
        String linkingInstance = "{'@type': '"+HBPVocabulary.LINKING_INSTANCE_TYPE+"', " +
                "'"+HBPVocabulary.LINKING_INSTANCE_FROM+"': {'@id': '"+configuration.getAbsoluteUrl(instance)+"'},"+
                "'"+HBPVocabulary.LINKING_INSTANCE_TO+"': {'@id': '"+configuration.getAbsoluteUrl(new NexusInstanceReference("bla", "bla", "bla", "v0.0.1", "foo"))+"'}}";

        Map map = jsonTransformer.parseToMap(linkingInstance);
        NexusInstanceReference editorInstance = nexusInstanceController.createInstanceByIdentifier(new NexusSchemaReference("foo", "core", "link", "v0.0.1"), "linkingInstance", new JsonDocument(map), null);
        IndexingMessage indexingMessage = new IndexingMessage(editorInstance, jsonTransformer.getMapAsJson(map), "2018-10-31", "Foo");
        TodoList inserted = graphIndexing.insert(indexingMessage);
        nexusInsertionTrigger(inserted);

    }

}
