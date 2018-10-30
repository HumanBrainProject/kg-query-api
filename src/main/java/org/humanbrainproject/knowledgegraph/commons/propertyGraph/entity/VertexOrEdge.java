package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import java.util.List;

public interface VertexOrEdge extends VertexOrEdgeReference{

    String getId();

    String getTypeName();

    List<Property>  getProperties();

    List<Edge> getEdges();

    MainVertex getMainVertex();
}
