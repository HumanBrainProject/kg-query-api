package org.humanbrainproject.knowledgegraph.releasing.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReleaseStatusResponseTest{


    private ReleaseStatusResponse releaseStatusResponse;

    @Before
    public void setUp(){
        releaseStatusResponse = new ReleaseStatusResponse();
    }

    @Test
    public void setId() {
        NexusInstanceReference reference = TestObjectFactory.fooInstanceReference();
        releaseStatusResponse.setId(reference);
        assertEquals("foo/bar/foobar/v0.0.1/barfoo", releaseStatusResponse.get("id"));
    }

    @Test
    public void setRootStatus() {

        releaseStatusResponse.setRootStatus(ReleaseStatus.NOT_RELEASED);

        assertEquals(ReleaseStatus.NOT_RELEASED.name(), releaseStatusResponse.get("status"));

        releaseStatusResponse.setRootStatus(null);

        assertFalse(releaseStatusResponse.containsKey("status"));

    }

    @Test
    public void setChildrenStatus() {

        releaseStatusResponse.setChildrenStatus(ReleaseStatus.NOT_RELEASED);

        assertEquals(ReleaseStatus.NOT_RELEASED.name(), releaseStatusResponse.get("childrenStatus"));

        releaseStatusResponse.setChildrenStatus(null);

        assertFalse(releaseStatusResponse.containsKey("childrenStatus"));

    }
}