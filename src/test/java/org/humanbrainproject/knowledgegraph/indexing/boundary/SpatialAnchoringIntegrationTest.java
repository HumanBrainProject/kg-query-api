package org.humanbrainproject.knowledgegraph.indexing.boundary;

import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@Ignore("IntegrationTest")
public class SpatialAnchoringIntegrationTest {

    @Autowired
    GraphIndexing graphIndexing;

    @Autowired
    NexusConfiguration configuration;

    @Test
    public void insert() {
       graphIndexing.insert(TestObjectFactory.createSpatialAnchoringIndexingMessage());
    }

}