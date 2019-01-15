package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NexusSchemaReferenceTest {

    NexusSchemaReference schemaReference;

    @Before
    public void setup(){
        this.schemaReference = NexusSchemaReference.createFromUrl("https://foo/v0/foo/core/bar/v1.0.0");
    }


    @Test
    public void createFromUrl() {
        Assert.assertEquals("foo", schemaReference.getOrganization());
        Assert.assertEquals("core", schemaReference.getDomain());
        Assert.assertEquals("bar", schemaReference.getSchema());
        Assert.assertEquals("v1.0.0", schemaReference.getSchemaVersion());
    }

    @Test
    public void createFromRelativeUrl(){
        NexusSchemaReference fromUrl = NexusSchemaReference.createFromUrl("foo/core/bar/v1.0.0");
        Assert.assertEquals("foo", fromUrl.getOrganization());
        Assert.assertEquals("core", fromUrl.getDomain());
        Assert.assertEquals("bar", fromUrl.getSchema());
        Assert.assertEquals("v1.0.0", fromUrl.getSchemaVersion());

    }


    @Test
    public void createUniqueNamespace() {
        String uniqueNamespace = schemaReference.createUniqueNamespace();
        Assert.assertEquals("https://schema.hbp.eu/foo/core/bar/v1.0.0/", uniqueNamespace);
    }

    @Test
    public void cloneInstance() {
        NexusSchemaReference clone = schemaReference.clone();

        Assert.assertFalse(clone == schemaReference);
        Assert.assertEquals(schemaReference, clone);
    }

    @Test
    public void getRelativeUrlForOrganization(){
        NexusRelativeUrl relativeUrlForOrganization = schemaReference.getRelativeUrlForOrganization();
        Assert.assertEquals("foo", relativeUrlForOrganization.getUrl());
    }

    @Test
    public void getRelativeUrlForDomain(){
        NexusRelativeUrl relativeUrl = schemaReference.getRelativeUrlForDomain();
        Assert.assertEquals("foo/core", relativeUrl.getUrl());
    }

    @Test
    public void getRelativeUrl(){
        NexusRelativeUrl relativeUrl = schemaReference.getRelativeUrl();
        Assert.assertEquals("foo/core/bar/v1.0.0", relativeUrl.getUrl());
    }

    @Test
    public void extractMainOrganization(){
        String mainOrg = NexusSchemaReference.extractMainOrganization("foo"+ SubSpace.EDITOR.getPostFix());
        Assert.assertEquals("foo", mainOrg);
    }

    @Test
    public void extractMainOrganizationNoPostfix(){
        String mainOrg = NexusSchemaReference.extractMainOrganization("foo");
        Assert.assertEquals("foo", mainOrg);
    }

    @Test
    public void isInSubspaceMain(){
        Assert.assertTrue(schemaReference.isInSubSpace(SubSpace.MAIN));
    }
    @Test
    public void isInSubspaceMainFalse(){
        Assert.assertFalse(schemaReference.isInSubSpace(SubSpace.EDITOR));
    }

    @Test
    public void isInSubspaceEditor(){
        NexusSchemaReference schemaReference = this.schemaReference.toSubSpace(SubSpace.EDITOR);
        Assert.assertTrue(schemaReference.isInSubSpace(SubSpace.EDITOR));
    }
    @Test
    public void isInSubspaceEditorFalse(){
        NexusSchemaReference schemaReference = this.schemaReference.toSubSpace(SubSpace.EDITOR);
        Assert.assertFalse(schemaReference.isInSubSpace(SubSpace.MAIN));
    }


}