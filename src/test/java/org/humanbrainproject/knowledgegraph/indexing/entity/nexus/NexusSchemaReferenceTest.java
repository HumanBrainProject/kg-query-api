/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

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