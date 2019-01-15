package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NexusRelativeUrlTest {

    NexusRelativeUrl relativeUrl;

    @Before
    public void setUp() throws Exception {
        this.relativeUrl = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, "foo");
    }

    @Test
    public void getUrl() {
        this.relativeUrl.addQueryParameter("foo", "bar");
        String url = this.relativeUrl.getUrl();
        Assert.assertEquals("foo?&foo=bar", url);

    }
}