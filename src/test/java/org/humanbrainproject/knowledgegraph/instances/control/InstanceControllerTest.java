package org.humanbrainproject.knowledgegraph.instances.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)
@Ignore("Integration test")
public class InstanceControllerTest {

    @Autowired
    InstanceManipulationController instanceController;

    @Autowired
    SystemOidcClient oidcClient;

    @Test
    public void create() throws IOException {
        NexusInstanceReference nexusInstanceReference = TestObjectFactory.fooInstanceReference();
        JsonDocument instance= new JsonDocument();
        instance.put(SchemaOrgVocabulary.NAME, "adfdasf");
        instance.put(HBPVocabulary.NAMESPACE+"foo", "barfas");
        instanceController.createInstanceByIdentifier(nexusInstanceReference.getNexusSchema(), "barfoo", instance);
    }
}