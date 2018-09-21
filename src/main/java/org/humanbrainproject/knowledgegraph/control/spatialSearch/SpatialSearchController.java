package org.humanbrainproject.knowledgegraph.control.spatialSearch;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.solr.SolrRepository;
import org.humanbrainproject.knowledgegraph.entity.indexing.SpatialAnchoring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpatialSearchController {


    @Autowired
    SolrRepository solrRepository;

    @Autowired
    ArangoRepository arangoRepository;

    public void index(SpatialAnchoring anchoring){
        if(anchoring!=null) {
            System.out.println("Indexing "+anchoring);
        }
    }

    public void remove(SpatialAnchoring spatialAnchoring) {


    }
}
