package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;

public class ResolvedVertexStructure {

    private final QualifiedIndexingMessage qualifiedMessage;
    private final MainVertex mainVertex;
    private final SubSpace targetSubSpace;


    public ResolvedVertexStructure(QualifiedIndexingMessage qualifiedMessage, MainVertex vertex, SubSpace targetSubSpace) {
        this.qualifiedMessage = qualifiedMessage;
        this.mainVertex = vertex;
        this.targetSubSpace = targetSubSpace;
    }

    public QualifiedIndexingMessage getQualifiedMessage() {
        return qualifiedMessage;
    }

    public MainVertex getMainVertex() {
        return mainVertex;
    }

}
