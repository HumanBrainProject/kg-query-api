package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.query.entity.BoundingBox;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore("This is an integration test")
public class SpatialSearchTest {

    @Test
    public void minimalBoundingBox() throws IOException, SolrServerException {
        BoundingBox box = new BoundingBox(0.0f, 0.0f, 0.0f, 0.1f, 0.1f, 0.1f, "space0.545534956419");
        new SpatialSearch().minimalBoundingBox(box);

    }
}