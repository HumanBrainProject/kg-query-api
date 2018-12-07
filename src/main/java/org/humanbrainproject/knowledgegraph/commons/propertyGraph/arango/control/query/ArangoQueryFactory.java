package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.UnauthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ArangoQueryFactory {


    @Autowired
    NexusConfiguration configuration;


    public String queryForIdsWithProperty(String propertyName, String propertyValue, Set<ArangoCollectionReference> collectionsToCheck, Set<String> permissionGroupsWithReadAccess) {
        return queryForValueWithProperty(propertyName, propertyValue,collectionsToCheck, ArangoVocabulary.ID, permissionGroupsWithReadAccess);
    }

    public String queryOutboundRelationsForDocument(ArangoDocumentReference document, Set<ArangoCollectionReference> edgeCollections, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.setParameter("documentId", document.getId());
        q.setTrustedParameter("edges", q.listCollections(edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())));
        q.addLine("LET doc = DOCUMENT(\"${documentId}\")");
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine("FOR v, e IN 1..1 OUTBOUND doc ${edges}");
        q.addDocumentFilter(new TrustedAqlValue("v"));
        q.addLine("RETURN e." + ArangoVocabulary.ID);
        return q.build().getValue();
    }

    public String queryOriginalIdForLink(ArangoDocumentReference document, ArangoCollectionReference linkReference,  Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.setParameter("documentId", document.getId());
        q.setParameter("edge", linkReference.getName());
        q.addLine("LET doc = DOCUMENT(\"${documentId}\") ");
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine("FOR v IN 1..1 INBOUND doc `${edge}`");
        q.addDocumentFilter(new TrustedAqlValue("v"));
        q.addLine("RETURN v."+ArangoVocabulary.NEXUS_RELATIVE_URL);

        return q.build().getValue();
    }

    public String queryForValueWithProperty(String propertyName, String propertyValue, Set<ArangoCollectionReference> collectionsToCheck, String lookupProperty, Set<String> permissionGroupsWithReadAccess) {
        if (collectionsToCheck != null && !collectionsToCheck.isEmpty()) {
            AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
            q.setTrustedParameter("collections", q.listCollections(collectionsToCheck.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())));

            for (ArangoCollectionReference arangoCollectionReference : collectionsToCheck) {
                AuthorizedArangoQuery subquery = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, true);
                subquery.setParameter("collectionName", arangoCollectionReference.getName());
                subquery.setParameter("propertyName", propertyName);
                subquery.setParameter("propertyValue", propertyValue);
                subquery.setParameter("lookupProperty", lookupProperty);

                subquery.addLine("LET `${collectionName}`= (").indent();
                subquery.addLine("FOR v IN `${collectionName}`").indent();
                subquery.addDocumentFilter(new TrustedAqlValue("v"));
                subquery.addLine("FILTER v.`${propertyName}` == \"${propertyValue}\" RETURN v.`${lookupProperty}`").outdent();
                subquery.outdent().addLine(")");
                q.addLine(subquery.build().getValue());
            }
            if(collectionsToCheck.size()>1){
                q.addLine("RETURN UNIQUE(UNION(${collections}))");
            }
            else{
                q.addLine("RETURN UNIQUE(${collections})");
            }
            return q.build().getValue();
        }
        return null;
    }

    @UnauthorizedAccess("We're returning information about specifications - this is meta information and non-sensitive")
    public String getAll(ArangoCollectionReference collection) {
        UnauthorizedArangoQuery q = new UnauthorizedArangoQuery();
        q.setParameter("collection", collection.getName());

        q.addLine("FOR doc IN `${collection}`").indent();
        q.addLine("RETURN doc");

        return q.build().getValue();
    }

    public String queryInDepthGraph(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference startDocument, Integer step, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);

        TrustedAqlValue edges = q.listCollections(edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()));

        AuthorizedArangoQuery outboundSubquery = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, true);

        outboundSubquery.setParameter("depth", String.valueOf(step));
        outboundSubquery.setTrustedParameter("edges", edges);

        outboundSubquery.addLine("FOR v, e, p IN 1..${depth} OUTBOUND doc ${edges}").indent();
        outboundSubquery.addDocumentFilter(new TrustedAqlValue("v"));
        outboundSubquery.addLine("RETURN p").outdent();

        AuthorizedArangoQuery inboundSubquery = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, true);
        inboundSubquery.setTrustedParameter("edges", edges);

        inboundSubquery.addLine("FOR v, e, p IN 1..1 INBOUND doc ${edges}").indent();
        inboundSubquery.addDocumentFilter(new TrustedAqlValue("v"));
        inboundSubquery.addLine("RETURN p").outdent();

        q.setParameter("documentId", startDocument.getId());
        q.setTrustedParameter("outbound", outboundSubquery.build());
        q.setTrustedParameter("inbound", inboundSubquery.build());

        q.addLine("LET doc = DOCUMENT(\"${documentId}\")");
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine("FOR path IN UNION_DISTINCT(").indent();
        q.addLine("(${outbound}), (${inbound})").outdent();
        q.addLine(")");
        q.addLine("RETURN path");
        return q.build().getValue();
    }

    @UnauthorizedAccess("Currently, this method is only applied to the internal database. Be cautious if sensitive information is going into the internal database and NEVER use it for other databases since this would be a vulnerability")
    public String getAllInternalDocumentsOfACollection(ArangoCollectionReference collection) {
        UnauthorizedArangoQuery q = new UnauthorizedArangoQuery();
        q.setParameter("collection", collection.getName());
        q.addLine("FOR spec IN `${collection}` RETURN spec");
        return q.build().getValue();
    }


    public String queryReleaseGraph(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference rootInstance, Integer maxDepth, Set<String> permissionGroupsWithReadAccess) {
        return childrenStatus(rootInstance, null, 0, maxDepth, edgeCollections, permissionGroupsWithReadAccess).getValue();
    }

    private TrustedAqlValue childrenStatus(ArangoDocumentReference rootInstance, String startingVertex, Integer level, Integer maxDepth, Set<ArangoCollectionReference> edgeCollections, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, level>0);
        String name = "level" + level;
        TrustedAqlValue childrenQuery = new TrustedAqlValue("[]");
        if (level < maxDepth) {
            childrenQuery = childrenStatus(rootInstance, name + "_doc", level + 1, maxDepth, edgeCollections, permissionGroupsWithReadAccess);
        }
        query.setParameter("name", "level" + level)
        .setParameter("startId", rootInstance.getId())
        .setParameter("doc", startingVertex)
        .setTrustedParameter("collections", query.listCollections(edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())))
        .setParameter("releaseInstanceRelation", ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE).getName())
        .setParameter("releaseState", HBPVocabulary.RELEASE_STATE)
        .setParameter("revision", HBPVocabulary.PROVENANCE_REVISION)
        .setTrustedParameter("childrenQuery", childrenQuery)
        .setParameter("schemaOrgName", SchemaOrgVocabulary.NAME)
        .setParameter("releaseInstanceProperty", HBPVocabulary.RELEASE_INSTANCE)
        .setParameter("releaseRevisionProperty", HBPVocabulary.RELEASE_REVISION)
        .setParameter("nexusBaseForInstances", configuration.getNexusBase(NexusConfiguration.ResourceType.DATA))
        .setParameter("originalId", ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV)
        .setParameter("releasedValue", ReleaseStatus.RELEASED.name())
        .setParameter("changedValue", ReleaseStatus.HAS_CHANGED.name())
        .setParameter("notReleasedValue", ReleaseStatus.NOT_RELEASED.name());

        for(int i=0; i<level; i++){
            query.indent();
        }

        if (level==0) {
            query.addLine("LET ${name}_doc = DOCUMENT(\"${startId}\")");
            query.addDocumentFilter(new TrustedAqlValue("${name}_doc"));
        } else {
            query.addLine("FOR ${name}_doc, ${name}_edge IN 1..1 OUTBOUND ${doc} ${collections}");
            query.addDocumentFilter(new TrustedAqlValue("${name}_doc"));
            query.addLine("SORT ${name}_doc.`" + JsonLdConsts.TYPE + "`, ${name}_doc.`${schemaOrgName}`");
        }
        query.addLine("LET ${name}_release = (FOR ${name}_status_doc IN 1..1 INBOUND ${name}_doc `${releaseInstanceRelation}`");
        query.indent();
        query.addLine("LET ${name}_release_instance = SUBSTITUTE(CONCAT(${name}_status_doc.`${releaseInstanceProperty}`.`" + JsonLdConsts.ID + "`, \"?rev=\", ${name}_status_doc.`${releaseRevisionProperty}`), \"${nexusBaseForInstances}/\", \"\")");
        query.addLine("RETURN ${name}_release_instance==${name}_doc.${originalId} ? \"${releasedValue}\" : \"${changedValue}\"");
        query.outdent();
        query.addLine(")");
        query.addLine("LET ${name}_status = LENGTH(${name}_release)>0 ? ${name}_release[0] : \"${notReleasedValue}\"");
        query.indent();
        query.addLine("LET ${name}_children = (");
        query.addLine("${childrenQuery}");
        query.addLine(")");
        query.outdent();
        if(level==0){
            query.addLine("RETURN MERGE({\"status\": ${name}_status, \"children\": ${name}_children, \"rev\": ${name}_doc.`${revision}` }, ${name}_doc)");
        } else {
            query.addLine("RETURN MERGE({\"status\": ${name}_status, \"children\": ${name}_children, \"linkType\": ${name}_edge._name, \"rev\": ${name}_doc.`${revision}`}, ${name}_doc)");
        }
        for(int i=0; i<level; i++){
            query.outdent();
        }
        return query.build();

    }

    public String getInstanceList(ArangoCollectionReference collection, Integer from, Integer size, String searchTerm, Set<String> permissionGroupsWithReadAccess, boolean sort) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, false);
        query.setParameter("collection", collection.getName());
        query.setParameter("from", from!=null ? from.toString(): null);
        query.setParameter("size", size!=null ? size.toString(): null);
        boolean hasSearchTerm = searchTerm != null && !searchTerm.isEmpty();
        query.setParameter("searchTerm", hasSearchTerm ? searchTerm.toLowerCase() : null);
        query.setParameter("filterProperty", SchemaOrgVocabulary.NAME);

        query.addLine("FOR doc IN `${collection}`");
        query.addDocumentFilter(new TrustedAqlValue("doc"));
        if (hasSearchTerm) {
            query.addLine("FILTER LIKE (LOWER(doc.`${filterProperty}`), \"%${searchTerm}%\")");
        }
        if(sort) {
            query.addLine("SORT doc.`${filterProperty}`");
        }
        if(size!=null){
            if (from != null) {
                query.addLine("LIMIT ${from}, ${size}");
            }
            else{
                query.addLine("LIMIT ${size}");
            }
        }
        query.addLine("RETURN doc");
        return query.build().getValue();
    }


    public String getBookmarks(NexusInstanceReference doc, Integer from, Integer size, String searchTerm, Set<String> permissionGroupsWithReadAccess){
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.setParameter("filterValue", doc.getFullId(false));
        boolean hasSearchTerm = searchTerm!=null && !searchTerm.trim().isEmpty();
        q.setParameter("searchTerm", hasSearchTerm ? searchTerm.toLowerCase() : null);
        q.setParameter("from", from != null ? String.valueOf(from) : null);
        q.setParameter("size", size != null ? String.valueOf(size) : null);

        q.addLine("FOR doc IN `hbpkg-core-bookmark-v0_0_1`").indent();
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine("FILTER CONTAINS(doc.`https://schema.hbp.eu/hbpkg/bookmarkList`.`https://schema.hbp.eu/relativeUrl`, \"${filterValue}\")").indent();
        q.addLine("LET instances = (").indent();
        q.addLine("FOR i IN 1..1 OUTBOUND doc `schema_hbp_eu-hbpkg-bookmarkInstanceLink`").indent();
        q.addDocumentFilter(new TrustedAqlValue("i"));
        q.addLine("FILTER i.`"+JsonLdConsts.ID+"` != NULL");
        if (searchTerm != null && !searchTerm.isEmpty()) {
            q.addLine("FILTER LIKE (LOWER(i.`"+SchemaOrgVocabulary.NAME+"`), \"%${searchTerm}%\")");
        }
        q.addLine("SORT i.`"+SchemaOrgVocabulary.NAME+"`");
        if (from != null && size != null) {
            q.addLine("LIMIT ${from}, ${size}");
        }
        q.addLine("RETURN {").indent();
        q.addLine("\"id\": i.`"+HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK+"`,");
        q.addLine("\"name\": i.`"+SchemaOrgVocabulary.NAME+"`,");
        q.addLine("\"dataType\": i.`"+JsonLdConsts.TYPE+"`,");
        q.addLine("\"description\": i.`"+SchemaOrgVocabulary.DESCRIPTION+"`");
        q.addLine("}").outdent();
        q.addLine(")").outdent();
        q.addLine("FILTER instances != null AND COUNT(instances) > 0").outdent();
        q.addLine("RETURN FIRST(instances)");
        return q.build().getValue();
    }
}
