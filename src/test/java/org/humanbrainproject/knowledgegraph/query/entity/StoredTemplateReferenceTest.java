package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Assert;
import org.junit.Test;

public class StoredTemplateReferenceTest {

    @Test
    public void getName() {
        StoredTemplateReference templateReference = new StoredTemplateReference(new StoredQueryReference(TestObjectFactory.fooInstanceReference().getNexusSchema(), "fooQuery"), "fooTemplate");
        Assert.assertEquals("foo-bar-foobar-v0_0_1-fooQuery/fooTemplate", templateReference.getName());
    }
}