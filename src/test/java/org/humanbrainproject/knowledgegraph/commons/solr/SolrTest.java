package org.humanbrainproject.knowledgegraph.commons.solr;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

@Ignore("Integration test")
public class SolrTest {

    @Test
    public void registerCore() throws IOException, SolrServerException {

        Solr solr = new Solr();
        solr.solrBase = "http://localhost:8983/solr";
        solr.solrCore = "foo4";
        solr.removeCore();
        solr.registerCore();
        solr.registerPoints("bar", "foobar", new HashSet<>(Arrays.asList(new ThreeDVector(0.1, 0.2, 0.3), new ThreeDVector(0.2, 0.4, 0.6))));

    }
}