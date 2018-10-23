package org.humanbrainproject.knowledgegraph.propertyGraph.entity;

import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

/**
 * An external edge points to a URL which is not part of the current Nexus environment
 */
public class ExternalEdge extends Edge {

    private final URL targetUrl;

    public ExternalEdge(String name, Vertex fromVertex, URL targetUrl, Integer orderNumber) {
        super(name, fromVertex, new ArrayList<>(), orderNumber);
        this.targetUrl = targetUrl;
    }

    public URL getTargetUrl() {
        return targetUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExternalEdge that = (ExternalEdge) o;
        return Objects.equals(targetUrl, that.targetUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetUrl);
    }
}
