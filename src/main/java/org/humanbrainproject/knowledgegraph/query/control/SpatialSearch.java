package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.solr.Mercator;
import org.humanbrainproject.knowledgegraph.commons.solr.Solr;
import org.humanbrainproject.knowledgegraph.query.entity.BoundingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequestScope
@Tested
public class SpatialSearch {

    @Autowired
    Solr solr;

    @Autowired
    Mercator mercator;

    private Map<BoundingBox, Set<ArangoDocumentReference>> cache = new HashMap<>();

    public Set<ArangoDocumentReference> minimalBoundingBox(BoundingBox box) throws IOException {
        if(cache.get(box)!=null){
            return cache.get(box);
        }
        else {
            List<String> ids = mercator.queryIdsOfMinimalBoundingBox(box);
            Set<ArangoDocumentReference> references = ids.stream().map(ArangoDocumentReference::fromId).collect(Collectors.toSet());
            cache.put(box, references);
            return references;
        }
    }

}
