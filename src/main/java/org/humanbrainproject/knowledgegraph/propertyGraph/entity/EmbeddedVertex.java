package org.humanbrainproject.knowledgegraph.propertyGraph.entity;

/**
 * An embedded vertex is a document originating from an embedded structure in a json-ld
 *
 */
public class EmbeddedVertex extends Vertex{

    private final EmbeddedEdge parentRelation;

    public EmbeddedVertex(EmbeddedEdge parentRelation) {
        this.parentRelation = parentRelation;
        this.parentRelation.setToVertex(this);
    }

    @Override
    String getInternalIdentifier() {
        return this.parentRelation.getIdentifier();
    }
}
