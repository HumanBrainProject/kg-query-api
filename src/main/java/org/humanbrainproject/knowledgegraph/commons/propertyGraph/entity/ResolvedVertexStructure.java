package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;

public class ResolvedVertexStructure {

    private final QualifiedIndexingMessage qualifiedMessage;
    private final MainVertex mainVertex;

    public ResolvedVertexStructure(QualifiedIndexingMessage qualifiedMessage, MainVertex vertex) {
        this.qualifiedMessage = qualifiedMessage;
        this.mainVertex = vertex;
    }

    public QualifiedIndexingMessage getQualifiedMessage() {
        return qualifiedMessage;
    }

    public MainVertex getMainVertex() {
        return mainVertex;
    }

}
