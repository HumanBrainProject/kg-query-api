package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

public class NexusInstanceReference implements InstanceReference {

    private final NexusSchemaReference nexusSchema;
    private final String id;
    private Integer revision;

    protected static Logger logger = LoggerFactory.getLogger(NexusInstanceReference.class);

    public static NexusInstanceReference createFromUrl(String url) {
        NexusSchemaReference schema = NexusSchemaReference.createFromUrl(url);
        if (schema != null) {
            String id = url.substring(url.indexOf(schema.getRelativeUrl().getUrl()) + schema.getRelativeUrl().getUrl().length()+1);
            UriComponents uri = UriComponentsBuilder.fromUriString(id).build();
            NexusInstanceReference reference = new NexusInstanceReference(schema, uri.getPath());
            if(uri.getQueryParams().containsKey("rev")){
                try{
                    reference.setRevision(Integer.parseInt(Objects.requireNonNull(uri.getQueryParams().getFirst("rev"))));
                }catch (NumberFormatException e){
                    logger.warn("Invalid revision number", e);
                }
            }
            return reference;
        }
        return null;
    }

    @Override
    public String getTypeName() {
        return nexusSchema.getRelativeUrl().getUrl();
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

    @Override
    public String getFullId() {
        return String.format("%s%s", getRelativeUrl().getUrl(), getRevision()==null ? "" : String.format("?rev=%d", getRevision()));
    }

    public NexusRelativeUrl getRelativeUrl() {
        return new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, String.format("%s/%s", nexusSchema.getRelativeUrl().getUrl(), id));
    }

    public NexusSchemaReference getNexusSchema() {
        return nexusSchema;
    }

    @Override
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

    @Override
    public String toString() {
        return  nexusSchema + "/" + id + "?rev=" + revision;
    }
}
