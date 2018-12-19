package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.query.entity.BoundingBox;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SpatialSearch {

    private SolrClient solr;

    private SolrClient getSolr() {
        if (solr == null) {
            solr = new HttpSolrClient.Builder("http://localhost:8983/solr/5000k").build();
        }
        return solr;


    }

    public Set<ArangoDocumentReference> minimalBoundingBox(BoundingBox box) throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery("*:*");
        String coordinateQuery = String.format("geometry.coordinates:[\"%s\" TO \"%s\"]", box.getFrom(), box.getTo());
        String referenceSpaceQuery = String.format("geometry.reference_space:\"%s\"", box.getReferenceSpace());
        query.setFilterQueries(coordinateQuery, referenceSpaceQuery);
        query.setFields("properties.id");
        query.setRows(0);
        QueryResponse response = getSolr().query(query);
        long matches = response.getResults().getNumFound();
        query.setRows(Long.valueOf(matches).intValue());
        query.setParam("group", true);
        query.setParam("group.field", "properties.id");
        response = getSolr().query(query);
        return response.getGroupResponse().getValues().get(0).getValues().stream().map(v -> ArangoDocumentReference.fromId(v.getGroupValue())).collect(Collectors.toSet());

    }


}
