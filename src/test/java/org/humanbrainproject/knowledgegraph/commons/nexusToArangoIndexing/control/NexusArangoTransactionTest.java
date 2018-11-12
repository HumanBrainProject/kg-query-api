package org.humanbrainproject.knowledgegraph.commons.nexusToArangoIndexing.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@Ignore("Integration test")
public class NexusArangoTransactionTest {


    @Autowired
    NexusArangoTransaction transaction;

    @Test
    public void createTypeLookup() {
        transaction.createTypeLookup(new NexusSchemaReference("foo", "bar", "foobar", "v0.0.1"));
    }
}