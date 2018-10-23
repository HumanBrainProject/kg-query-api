package org.humanbrainproject.knowledgegraph.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;

import java.util.Objects;

public class ArangoCollectionReference {


    private final String name;

    public ArangoCollectionReference(String collectionName) {
        this.name = collectionName;
    }

    public static ArangoCollectionReference fromNexusSchemaReference(NexusSchemaReference path){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(path.getRelativeUrl())));
    }

    public static ArangoCollectionReference fromSpecTraversal(SpecTraverse specTraverse){
        return new ArangoCollectionReference(ArangoNamingHelper.replaceSpecialCharacters(String.format("rel-%s", ArangoNamingHelper.reduceStringToMaxSizeByHashing(specTraverse.pathName))));
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
