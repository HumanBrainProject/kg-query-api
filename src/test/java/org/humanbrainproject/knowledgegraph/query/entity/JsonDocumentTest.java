package org.humanbrainproject.knowledgegraph.query.entity;

import org.junit.Assert;
import org.junit.Test;

public class JsonDocumentTest {

    @Test
    public void removeAllInternalKeys() {
        JsonDocument document = new JsonDocument();
        document.addToProperty("_foo", "bar");
        document.addToProperty("bar", "foo");
        document.removeAllInternalKeys();

        Assert.assertEquals(1, document.size());
        Assert.assertEquals("foo", document.get("bar"));
    }
}