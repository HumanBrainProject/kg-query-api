package org.humanbrainproject.knowledgegraph.releasing.entity;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseStatusTest {

    @Test
    public void isWorseThan() {
        boolean worseThan = ReleaseStatus.NOT_RELEASED.isWorseThan(ReleaseStatus.RELEASED);
        Assert.assertTrue(worseThan);
    }

    @Test
    public void isNotWorseThan() {
        boolean worseThan = ReleaseStatus.HAS_CHANGED.isWorseThan(ReleaseStatus.NOT_RELEASED);
        Assert.assertFalse(worseThan);
    }

    @Test
    public void isWorst() {
        Assert.assertTrue(ReleaseStatus.NOT_RELEASED.isWorst());
    }

    @Test
    public void isNotWorst() {
        Assert.assertFalse(ReleaseStatus.HAS_CHANGED.isWorst());
        Assert.assertFalse(ReleaseStatus.RELEASED.isWorst());
    }
}