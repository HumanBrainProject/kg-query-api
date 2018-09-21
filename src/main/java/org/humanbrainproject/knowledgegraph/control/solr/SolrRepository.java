package org.humanbrainproject.knowledgegraph.control.solr;

import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.stereotype.Component;

@Component
public class SolrRepository {

    public void indexDocument(JsonLdVertex spatialSearchVertex){
        System.out.println("SOLR: Indexing "+spatialSearchVertex.getEntityName());
    }

    public void deleteByKeyAndReferenceSpace(String key, String referenceSpace){
        System.out.println("SOLR: Delete "+key+" "+referenceSpace);
    }

}
