/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NexusInstanceReferenceTest {

    NexusInstanceReference instanceFromMainSpace;

    @Before
    public void setup(){
        instanceFromMainSpace = new NexusInstanceReference("foo", "bar", "foobar", "v0.0.1", "barfoo");
    }

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
    public void createEditorInstanceReferenceByToSubSpace() {
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

    @Test
    public void getFullId(){
        String fullId = instanceFromMainSpace.getFullId(false);
        Assert.assertEquals("foo/bar/foobar/v0.0.1/barfoo", fullId);
    }

    @Test
    public void getFullIdWithImplicitRevision(){
        String fullId = instanceFromMainSpace.getFullId(true);
        Assert.assertEquals("foo/bar/foobar/v0.0.1/barfoo?rev=1", fullId);
    }

    @Test
    public void getFullIdWithExplicitRevision(){
        instanceFromMainSpace.setRevision(20);
        String fullId = instanceFromMainSpace.getFullId(true);
        Assert.assertEquals("foo/bar/foobar/v0.0.1/barfoo?rev=20", fullId);
    }

    @Test
    public void isSameInstanceRegardlessOfRevision(){
        NexusInstanceReference clone = instanceFromMainSpace.clone();
        clone.setRevision(30);
        Assert.assertTrue(instanceFromMainSpace.isSameInstanceRegardlessOfRevision(clone));
    }

    @Test
    public void isSameInstanceRegardlessOfRevisionDifferentId(){
        Assert.assertFalse(instanceFromMainSpace.isSameInstanceRegardlessOfRevision(new NexusInstanceReference(instanceFromMainSpace.getNexusSchema(), "bar")));
    }



    @Test
    public void cloneInstance(){
        instanceFromMainSpace.setRevision(20);
        NexusInstanceReference clone = instanceFromMainSpace.clone();
        Assert.assertFalse(clone == instanceFromMainSpace);
        Assert.assertFalse(clone.getNexusSchema()==instanceFromMainSpace.getNexusSchema());
        Assert.assertEquals(instanceFromMainSpace, clone);
        Assert.assertEquals(instanceFromMainSpace.getNexusSchema(), clone.getNexusSchema());
        Assert.assertEquals(instanceFromMainSpace.getRevision(), clone.getRevision());
    }

}