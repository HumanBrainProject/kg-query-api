package org.humanbrainproject.knowledgegraph.commons.solr;

import java.util.List;

public class MercatorFilter {
    private List<List<Double>> viewPort;
    private String referenceSpace;

    public MercatorFilter() {
    }

    public List<List<Double>> getViewPort() {
        return viewPort;
    }

    public void setViewPort(List<List<Double>> viewPort) {
        this.viewPort = viewPort;
    }

    public String getReferenceSpace() {
        return referenceSpace;
    }

    public void setReferenceSpace(String space) {
        this.referenceSpace = space;
    }
}
