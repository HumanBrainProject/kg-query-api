package org.humanbrainproject.knowledgegraph.nexusExt.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
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
public class SchemaControllerTest {

    @Autowired
    SchemaController schemaController;

    @Test
    public void create() throws IOException {
        NexusSchemaReference nexusSchemaReference = TestObjectFactory.fooInstanceReference().getNexusSchema();
        schemaController.createSchema(nexusSchemaReference);
    }
}