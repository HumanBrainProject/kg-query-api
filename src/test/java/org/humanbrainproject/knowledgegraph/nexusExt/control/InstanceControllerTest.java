package org.humanbrainproject.knowledgegraph.nexusExt.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.OidcClient;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
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
public class InstanceControllerTest {

    @Autowired
    InstanceController instanceController;

    @Autowired
    OidcClient oidcClient;

    @Test
    public void create() throws IOException {
        NexusInstanceReference nexusInstanceReference = TestObjectFactory.fooInstanceReference();
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put(SchemaOrgVocabulary.NAME, "adfdasf");
        instance.put(HBPVocabulary.NAMESPACE+"foo", "barfas");
        instanceController.createInstanceByIdentifier(nexusInstanceReference.getNexusSchema(), "barfoo", instance, oidcClient.getAuthorizationToken());
    }
}