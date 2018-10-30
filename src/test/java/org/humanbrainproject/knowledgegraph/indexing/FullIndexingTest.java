package org.humanbrainproject.knowledgegraph.indexing;

import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.OidcClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.nexusExt.control.InstanceController;
import org.humanbrainproject.knowledgegraph.nexusExt.control.ReleaseController;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
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
    OidcClient oidcClient;

    Map<String, Object> payload;
    NexusInstanceReference instance;

    @Test
    public void createAndRelease() throws IOException {
        payload = new LinkedHashMap<>();
        payload.put(SchemaOrgVocabulary.NAMESPACE+"foo", "bar");
        instance = nexusInstanceController.createInstanceByIdentifier(TestObjectFactory.fooInstanceReference().getNexusSchema(), "helloWorld", payload,oidcClient.getAuthorizationToken());

        //This trigger is typically done by Nexus itself - we're simulating the behavior.
        IndexingMessage indexingMessage = new IndexingMessage(instance, new Gson().toJson(payload));
        graphIndexing.insert(indexingMessage);

        IndexingMessage releaseMessage = releaseController.release(instance, instance.getRevision(), oidcClient.getAuthorizationToken());

        //Again the simulation of the indexing trigger
        graphIndexing.insert(releaseMessage);

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
        releaseController.unrelease(instance, oidcClient.getAuthorizationToken());
    }


}
