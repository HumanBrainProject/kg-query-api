package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ReferenceType;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.VertexOrEdgeReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;

import java.util.Objects;
import java.util.Set;

public class ArangoCollectionReference {


    private final String name;

    public ArangoCollectionReference(String collectionName) {
        this.name = collectionName;
    }

    public static ArangoCollectionReference fromFieldName(String fieldName, ReferenceType referenceType){
        return new ArangoCollectionReference(String.format("%s-%s", referenceType.getPrefix(), ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(ArangoNamingHelper.removeTrailingHttps(fieldName)))));

    }

    public static ArangoCollectionReference fromNexusSchemaReference(NexusSchemaReference path){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(path.getRelativeUrl().getUrl())));
    }

    public static ArangoCollectionReference fromVertexOrEdgeReference(VertexOrEdgeReference vertexOrEdge){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(ArangoNamingHelper.removeTrailingHttps(vertexOrEdge.getTypeName()))));
    }

    public static ArangoCollectionReference fromSpecTraversal(SpecTraverse specTraverse, Set<ArangoCollectionReference> existingCollections){
        for (ReferenceType referenceType : ReferenceType.values()) {
            ArangoCollectionReference reference = new ArangoCollectionReference(String.format("%s-%s", referenceType.getPrefix(), ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(specTraverse.pathName))));
            if(existingCollections.contains(reference)){
                return reference;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoCollectionReference that = (ArangoCollectionReference) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
