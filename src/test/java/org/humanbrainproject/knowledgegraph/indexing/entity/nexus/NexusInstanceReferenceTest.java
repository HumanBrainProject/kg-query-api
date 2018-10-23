package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.propertyGraph.entity.SubSpace;
import org.junit.Assert;
import org.junit.Test;

public class NexusInstanceReferenceTest {

    NexusInstanceReference instanceFromMainSpace = new NexusInstanceReference("foo", "bar", "foobar", "v0.0.1", "barfoo");

    @Test
    public void createMainInstanceReference() {
        Assert.assertEquals("foo", instanceFromMainSpace.getNexusSchema().getOrganization());
        Assert.assertEquals("bar", instanceFromMainSpace.getNexusSchema().getDomain());
        Assert.assertEquals("foobar", instanceFromMainSpace.getNexusSchema().getSchema());
        Assert.assertEquals("v0.0.1", instanceFromMainSpace.getNexusSchema().getSchemaVersion());
        Assert.assertEquals("barfoo", instanceFromMainSpace.getId());
        Assert.assertEquals("foo/bar/foobar/v0.0.1/barfoo", instanceFromMainSpace.getRelativeUrl());
        Assert.assertEquals("foo/bar/foobar/v0.0.1", instanceFromMainSpace.getNexusSchema().getRelativeUrl());
        Assert.assertEquals(SubSpace.MAIN, instanceFromMainSpace.getNexusSchema().getSubSpace());
    }


    @Test
    public void createEditorInstanceReference() {
        NexusInstanceReference editorInstance = instanceFromMainSpace.toSubSpace(SubSpace.EDITOR);
        NexusInstanceReference newEditorInstance = new NexusInstanceReference("fooeditor", "bar", "foobar", "v0.0.1", "barfoo");
        Assert.assertEquals("foo", editorInstance.getNexusSchema().getOrganization());
        Assert.assertEquals("bar", editorInstance.getNexusSchema().getDomain());
        Assert.assertEquals("foobar", editorInstance.getNexusSchema().getSchema());
        Assert.assertEquals("v0.0.1", editorInstance.getNexusSchema().getSchemaVersion());
        Assert.assertEquals("barfoo", editorInstance.getId());
        Assert.assertEquals("foo"+SubSpace.EDITOR.getPostFix()+"/bar/foobar/v0.0.1/barfoo", editorInstance.getRelativeUrl());
        Assert.assertEquals("foo"+SubSpace.EDITOR.getPostFix()+"/bar/foobar/v0.0.1", editorInstance.getNexusSchema().getRelativeUrl());
        Assert.assertEquals(editorInstance, newEditorInstance);
    }


}