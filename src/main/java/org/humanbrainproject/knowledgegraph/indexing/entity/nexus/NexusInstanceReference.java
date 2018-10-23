package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.SubSpace;

import java.util.Objects;

public class NexusInstanceReference implements InstanceReference {

    private final NexusSchemaReference nexusSchema;
    private final String id;
    private Integer revision;

    public static NexusInstanceReference createFromUrl(String url) {
        NexusSchemaReference schema = NexusSchemaReference.createFromUrl(url);
        if (schema != null) {
            String id = url.substring(url.indexOf(schema.getRelativeUrl()) + schema.getRelativeUrl().length()+1);
            id = id.replaceAll("[?#].*", "");
            return new NexusInstanceReference(schema, id);
        }
        return null;
    }

    @Override
    public String getInternalIdentifier() {
        return getRelativeUrl();
    }

    public NexusInstanceReference(NexusSchemaReference nexusSchema, String id) {
        this.nexusSchema = nexusSchema;
        this.id = id;
    }


    public NexusInstanceReference toSubSpace(SubSpace subSpace) {
        return new NexusInstanceReference(nexusSchema.toSubSpace(subSpace), id);
    }

    public NexusInstanceReference(String organization, String domain, String schema, String schemaVersion, String id) {
        this(new NexusSchemaReference(organization, domain, schema, schemaVersion), id);
    }

    public String getRelativeUrl() {
        return String.format("%s/%s", nexusSchema.getRelativeUrl(), id);
    }

    public NexusSchemaReference getNexusSchema() {
        return nexusSchema;
    }

    public String getId() {
        return id;
    }

    public Integer getRevision() {
        return revision;
    }

    public NexusInstanceReference setRevision(Integer revision) {
        this.revision = revision;
        return this;
    }

    @Override
    public SubSpace getSubspace() {
        return getNexusSchema().getSubSpace();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NexusInstanceReference that = (NexusInstanceReference) o;
        return Objects.equals(nexusSchema, that.nexusSchema) &&
                Objects.equals(id, that.id) &&
                Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {

        return Objects.hash(nexusSchema, id, revision);
    }

    @Override
    public String createUniqueNamespace() {
        return getNexusSchema().createUniqueNamespace();
    }
}
