package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;

import java.util.Objects;

@Tested
public class NexusSchemaReference {

    private final String organization;
    private final SubSpace subSpace;
    private final String domain;
    private final String schema;
    private final String schemaVersion;

    private final static String VERSION_DECLARATION = "v\\d*\\.\\d*\\.\\d*";


    public static NexusSchemaReference createFromUrl(String url) {
        if (url != null && url.matches(".*" + VERSION_DECLARATION + ".*")) {
            String relativePath = url.replaceAll(".*/(?=.*/.*/.*/" + VERSION_DECLARATION + ")", "");
            String[] split = relativePath.split("/");
            if (split.length > 3) {
                return new NexusSchemaReference(split[0], split[1], split[2], split[3]);
            }
        }
        return null;
    }

    private NexusSchemaReference(String organization, SubSpace subSpace, String domain, String schema, String schemaVersion){
        this.organization = organization;
        this.subSpace = subSpace;
        this.domain = domain;
        this.schema = schema;
        this.schemaVersion = schemaVersion;
    }

    public NexusSchemaReference(String organization, String domain, String schema, String schemaVersion) {
        this(extractMainOrganization(organization), SubSpace.byPostfix(organization.substring(extractMainOrganization(organization).length())), domain, schema, schemaVersion);
    }

    public NexusSchemaReference toSubSpace(SubSpace subSpace){
        return new NexusSchemaReference(organization, subSpace, domain, schema, schemaVersion);
    }

    public String getOrganization() {
        return organization;
    }

    public SubSpace getSubSpace() {
        return subSpace;
    }

    public String getDomain() {
        return domain;
    }

    public String getSchema() {
        return schema;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public NexusRelativeUrl getRelativeUrlForOrganization() {
        return new NexusRelativeUrl(NexusConfiguration.ResourceType.ORGANIZATION, String.format("%s%s", getOrganization(), subSpace != null ? subSpace.getPostFix() : ""));
    }

    public NexusRelativeUrl getRelativeUrlForDomain(){
        return new NexusRelativeUrl(NexusConfiguration.ResourceType.DOMAIN, String.format("%s/%s", getRelativeUrlForOrganization().getUrl(), getDomain()));
    }

    public NexusRelativeUrl getRelativeUrl() {
        return new NexusRelativeUrl(NexusConfiguration.ResourceType.SCHEMA, String.format("%s/%s/%s", getRelativeUrlForDomain().getUrl(), getSchema(), getSchemaVersion()));
    }

    public NexusRelativeUrl getRelativeUrlForContext() {
        return new NexusRelativeUrl(NexusConfiguration.ResourceType.CONTEXT, String.format("%s/%s/%s", getRelativeUrlForDomain().getUrl(), getSchema(), getSchemaVersion()));
    }

    static String extractMainOrganization(String organization) {
        String result = organization;
        for (SubSpace subSpaceName : SubSpace.values()) {
            if (result.endsWith(subSpaceName.getPostFix())) {
                result = result.substring(0, result.length() - subSpaceName.getPostFix().length());
            }
        }
        return result;
    }

    public boolean isInSubSpace(SubSpace subSpace){
        return this.subSpace == subSpace;
    }

    public String createUniqueNamespace() {
        return String.format("https://schema.hbp.eu/%s/%s/%s/%s/", organization, domain, schema, schemaVersion);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NexusSchemaReference that = (NexusSchemaReference) o;
        return Objects.equals(organization, that.organization) &&
                subSpace == that.subSpace &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(schema, that.schema) &&
                Objects.equals(schemaVersion, that.schemaVersion);
    }

    @Override
    public int hashCode() {

        return Objects.hash(organization, subSpace, domain, schema, schemaVersion);
    }

    @Override
    public String toString() {
        return organization + (subSpace != null ? subSpace.getPostFix() : "") + "/" + domain + '/' + schema + '/' + schemaVersion;
    }

    public NexusSchemaReference clone(){
        return new NexusSchemaReference(organization, subSpace, domain, schema, schemaVersion);
    }

}
