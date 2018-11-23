package org.humanbrainproject.knowledgegraph.releasing.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@Ignore("integration test")
public class ReleaseControlTest {

    @Autowired
    ReleaseControl releaseControl;


    @Test
    public void findNexusInstanceFromInferredArangoEntry() {
        NexusInstanceReference nexusInstanceFromInferredArangoEntry = releaseControl.findNexusInstanceFromInferredArangoEntry(ArangoDocumentReference.fromId("foo-bar-foobar-v0_0_1/4f818243-6b5d-4e07-a834-925d1f769b64"), new OidcAccessToken());
        System.out.println(nexusInstanceFromInferredArangoEntry);
    }
}