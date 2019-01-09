package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.solr.Solr;
import org.humanbrainproject.knowledgegraph.query.entity.BoundingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ToBeTested(easy = true)
public class SpatialSearch {

    @Autowired
    Solr solr;

    public Set<ArangoDocumentReference> minimalBoundingBox(BoundingBox box) throws IOException, SolrServerException {
        List<String> ids = solr.queryIdsOfMinimalBoundingBox(box);
        return ids.stream().map(ArangoDocumentReference::fromId).collect(Collectors.toSet());
    }


}
