package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.junit.Assert;
import org.junit.Test;

public class NexusInstanceReferenceTest {

    NexusInstanceReference instanceFromMainSpace = new NexusInstanceReference("foo", "bar", "foobar", "v0.0.1", "barfoo");

    @Test
    public void createNexusInstanceReferenceWithRevisionFromUrl(){
        NexusInstanceReference result = NexusInstanceReference.createFromUrl("foo/bar/foobar/v0.0.1/barfoo?rev=12");
        Assert.assertEquals("foo", result.getNexusSchema().getOrganization());
        Assert.assertEquals("bar", result.getNexusSchema().getDomain());
        Assert.assertEquals("foobar", result.getNexusSchema().getSchema());
        Assert.assertEquals("v0.0.1", result.getNexusSchema().getSchemaVersion());
        Assert.assertEquals("barfoo", result.getId());
        Assert.assertEquals(Integer.valueOf(12), result.getRevision());

    }



    @Test
    public void createMainInstanceReference() {
        Assert.assertEquals("foo", instanceFromMainSpace.getNexusSchema().getOrganization());
        Assert.assertEquals("bar", instanceFromMainSpace.getNexusSchema().getDomain());
        Assert.assertEquals("foobar", instanceFromMainSpace.getNexusSchema().getSchema());
        Assert.assertEquals("v0.0.1", instanceFromMainSpace.getNexusSchema().getSchemaVersion());
        Assert.assertEquals("barfoo", instanceFromMainSpace.getId());
        Assert.assertEquals("foo/bar/foobar/v0.0.1/barfoo", instanceFromMainSpace.getRelativeUrl().getUrl());
        Assert.assertEquals("foo/bar/foobar/v0.0.1", instanceFromMainSpace.getNexusSchema().getRelativeUrl().getUrl());
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
        Assert.assertEquals("foo"+SubSpace.EDITOR.getPostFix()+"/bar/foobar/v0.0.1/barfoo", editorInstance.getRelativeUrl().getUrl());
        Assert.assertEquals("foo"+SubSpace.EDITOR.getPostFix()+"/bar/foobar/v0.0.1", editorInstance.getNexusSchema().getRelativeUrl().getUrl());
        Assert.assertEquals(editorInstance, newEditorInstance);
    }


}