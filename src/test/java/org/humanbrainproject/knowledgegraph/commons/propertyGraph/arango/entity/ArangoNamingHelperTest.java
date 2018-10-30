package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.junit.Assert;
import org.junit.Test;

public class ArangoNamingHelperTest {

    @Test
    public void removeTrailingHttp() {
        String result = ArangoNamingHelper.removeTrailingHttps("http://foo/bar");
        Assert.assertEquals("foo/bar", result);
    }
    @Test
    public void removeTrailingHttps() {
        String result = ArangoNamingHelper.removeTrailingHttps("https://foo/bar");
        Assert.assertEquals("foo/bar", result);
    }
}