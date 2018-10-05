package org.humanbrainproject.knowledgegraph.control.jsonld;

import org.junit.Assert;
import org.junit.Test;

public class JsonLdToVerticesAndEdgesTest {

    @Test
    public void translateReferenceToMainspaceEditor() {

        JsonLdToVerticesAndEdges instance = new JsonLdToVerticesAndEdges();

        String result = instance.translateReferenceToMainspace("http://someurl/downthepath/orgeditor/core/foo/v0.0.1/id");

        Assert.assertEquals("http://someurl/downthepath/org/core/foo/v0.0.1/id", result);

    }

    @Test
    public void translateReferenceToMainspaceReconciled() {

        JsonLdToVerticesAndEdges instance = new JsonLdToVerticesAndEdges();

        String result = instance.translateReferenceToMainspace("fooreconciled/core/foo/v0.0.1");

        Assert.assertEquals("foo/core/foo/v0.0.1", result);
    }

    @Test
    public void translateReferenceToMainspaceOriginal() {

        JsonLdToVerticesAndEdges instance = new JsonLdToVerticesAndEdges();

        String result = instance.translateReferenceToMainspace("foo/core/foo/v0.0.1");

        Assert.assertEquals("foo/core/foo/v0.0.1", result);

    }

}