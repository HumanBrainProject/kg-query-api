package org.humanbrainproject.knowledgegraph.control.spatialSearch;

import org.humanbrainproject.knowledgegraph.control.solr.SolrRepository;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SpatialSearchController {

    @Autowired
    SolrRepository solrRepository;


    public boolean isRelevant(List<JsonLdVertex> jsonLdVertices){
        return false;
    }

    public void remove(Map instance) {

    }

    public boolean isRelevant(Map instance){
        return false;
    }

    public void index(List<JsonLdVertex> jsonLdVertices){

    }

    public List<List<String>> findDocumentsToBeRemovedFromSpatialSearch(List<JsonLdVertex> jsonLdVertices){
        return Collections.emptyList();
    }

    public void remove(List<List<String>> elementsToBeRemovedFromSpatialSearch) {
    }
}
