package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

/**
 * An embedded vertex is a document originating from an embedded structure in a json-ld
 *
 */
public class EmbeddedVertex extends Vertex{

    public static final String NAME_PREFIX = "embins-";

    private final EmbeddedEdge parentRelation;

    public EmbeddedVertex(EmbeddedEdge parentRelation) {
        this.parentRelation = parentRelation;
        this.parentRelation.setToVertex(this);
    }

    @Override
    public MainVertex getMainVertex(){
        return parentRelation.getMainVertex();
    }

    @Override
    public String getId() {
        return parentRelation.getId();
    }

    @Override
    public String getTypeName() {
        return String.format("%s%s", NAME_PREFIX, parentRelation.getName());
    }


}
