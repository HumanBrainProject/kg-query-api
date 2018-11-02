package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.EdgeX;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

import java.util.Objects;

public class ArangoDocumentReference {

    private final ArangoCollectionReference collection;
    private final String key;

    public ArangoDocumentReference(ArangoCollectionReference collection, String key) {
        this.key = key;
        this.collection = collection;
    }

    public static ArangoDocumentReference fromEdge(EdgeX edge){
        ArangoCollectionReference collection = ArangoCollectionReference.fromEdge(edge);
        return new ArangoDocumentReference(collection, ArangoNamingHelper.createCompatibleId(edge.getId()));
    }

    public static ArangoDocumentReference fromId(String id) {
        String[] split = id.split("/");
        if (split.length == 2) {
            return new ArangoDocumentReference(new ArangoCollectionReference(split[0]), split[1]);
        }
        return null;
    }

    public static ArangoDocumentReference fromNexusInstance(NexusInstanceReference path) {
        return fromNexusInstance(path, null);

    }

    public static ArangoDocumentReference fromNexusInstance(NexusInstanceReference path, SubSpace subSpace) {
        NexusSchemaReference nexusSchema = path.getNexusSchema();
        if (subSpace != null) {
            nexusSchema = nexusSchema.toSubSpace(subSpace);
        }
        ArangoCollectionReference collection = ArangoCollectionReference.fromNexusSchemaReference(nexusSchema);
        return new ArangoDocumentReference(collection, path.getId());
    }

    public String getKey() {
        return key;
    }

    public ArangoCollectionReference getCollection() {
        return collection;
    }


    public String getId() {
        return String.format("%s/%s", getCollection().getName(), getKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoDocumentReference that = (ArangoDocumentReference) o;
        return Objects.equals(collection, that.collection) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, key);
    }
}
