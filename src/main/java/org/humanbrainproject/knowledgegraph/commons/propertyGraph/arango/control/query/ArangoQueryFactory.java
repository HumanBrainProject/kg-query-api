package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.UnauthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.commons.suggestion.SuggestionStatus;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.Filter;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

@Component
@ToBeTested
public class ArangoQueryFactory {


    @Autowired
    NexusConfiguration configuration;


    public String queryForIdsWithProperty(String propertyName, String propertyValue, Set<ArangoCollectionReference> collectionsToCheck, Set<String> permissionGroupsWithReadAccess) {
        return queryForValueWithProperty(propertyName, propertyValue, collectionsToCheck, ArangoVocabulary.ID, permissionGroupsWithReadAccess);
    }

    public String queryOutboundRelationsForDocument(ArangoDocumentReference document, Set<ArangoCollectionReference> edgeCollections, Set<String> permissionGroupsWithReadAccess) {
        return queryDirectRelationsForDocument(document, edgeCollections, permissionGroupsWithReadAccess, true);
    }

    public String queryInboundRelationsForDocument(ArangoDocumentReference document, Set<ArangoCollectionReference> edgeCollections, Set<String> permissionGroupsWithReadAccess) {
        return queryDirectRelationsForDocument(document, edgeCollections, permissionGroupsWithReadAccess, false);
    }

    public String queryLinkingInstanceBetweenVertices(ArangoDocumentReference from, ArangoDocumentReference to, ArangoCollectionReference relation, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.addLine(trust("FOR doc in `${collection}`"));
        q.addLine(trust("FILTER doc._from == \"${fromId}\""));
        q.addLine(trust("AND doc._to == \"${toId}\""));
        q.addLine(trust("RETURN doc"));
        q.setParameter("collection", relation.getName());
        q.setParameter("fromId", from.getId());
        q.setParameter("toId", to.getId());
        return q.build().getValue();
    }


    private String queryDirectRelationsForDocument(ArangoDocumentReference document, Set<ArangoCollectionReference> edgeCollections, Set<String> permissionGroupsWithReadAccess, boolean outbound) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.setParameter("documentId", document.getId());
        q.setParameter("direction", outbound ? "OUTBOUND" : "INBOUND");
        q.setTrustedParameter("edges", q.listCollections(edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())));
        q.addLine(trust("LET doc = DOCUMENT(\"${documentId}\")"));
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine(trust("FOR v, e IN 1..1 ${direction} doc ${edges}"));
        q.addDocumentFilter(new TrustedAqlValue("v"));
        q.addLine(trust("RETURN e." + ArangoVocabulary.ID));
        return q.build().getValue();
    }

    public String queryOriginalIdForLink(ArangoDocumentReference document, ArangoCollectionReference linkReference, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.setParameter("documentId", document.getId());
        q.setParameter("edge", linkReference.getName());
        q.addLine(trust("LET doc = DOCUMENT(\"${documentId}\") "));
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine(trust("FOR v IN 1..1 INBOUND doc `${edge}`"));
        q.addDocumentFilter(new TrustedAqlValue("v"));
        q.addLine(trust("RETURN v." + ArangoVocabulary.NEXUS_RELATIVE_URL));

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

                subquery.addLine(trust("LET `${collectionName}`= (")).indent();
                subquery.addLine(trust("FOR v IN `${collectionName}`")).indent();
                subquery.addDocumentFilter(new TrustedAqlValue("v"));
                subquery.addLine(trust("FILTER v.`${propertyName}` == \"${propertyValue}\" OR \"${propertyValue}\" IN v.`${propertyName}` RETURN v.`${lookupProperty}`")).outdent();
                subquery.outdent().addLine(trust(")"));
                q.addLine(trust(subquery.build().getValue()));
            }
            if (collectionsToCheck.size() > 1) {
                q.addLine(trust("RETURN UNIQUE(UNION(${collections}))"));
            } else {
                q.addLine(trust("RETURN UNIQUE(${collections})"));
            }
            return q.build().getValue();
        }
        return null;
    }

    @UnauthorizedAccess("We're returning information about specifications - this is meta information and non-sensitive")
    public String getAll(ArangoCollectionReference collection) {
        AQL q = new AQL();
        q.setParameter("collection", collection.getName());

        q.addLine(trust("FOR doc IN `${collection}`")).indent();
        q.addLine(trust("RETURN doc"));

        return q.build().getValue();
    }

    public String queryInDepthGraph(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference startDocument, Integer step, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);

        TrustedAqlValue edges = q.listCollections(edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()));

        AuthorizedArangoQuery outboundSubquery = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, true);

        outboundSubquery.setParameter("depth", String.valueOf(step));
        outboundSubquery.setTrustedParameter("edges", edges);

        outboundSubquery.addLine(trust("FOR v, e, p IN 1..${depth} OUTBOUND doc ${edges}")).indent();
        outboundSubquery.addDocumentFilter(new TrustedAqlValue("v"));
        outboundSubquery.addLine(trust("RETURN p")).outdent();

        AuthorizedArangoQuery inboundSubquery = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, true);
        inboundSubquery.setTrustedParameter("edges", edges);

        inboundSubquery.addLine(trust("FOR v, e, p IN 1..1 INBOUND doc ${edges}")).indent();
        inboundSubquery.addDocumentFilter(new TrustedAqlValue("v"));
        inboundSubquery.addLine(trust("RETURN p")).outdent();

        q.setParameter("documentId", startDocument.getId());
        q.setTrustedParameter("outbound", outboundSubquery.build());
        q.setTrustedParameter("inbound", inboundSubquery.build());

        q.addLine(trust("LET doc = DOCUMENT(\"${documentId}\")"));
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine(trust("FOR path IN UNION_DISTINCT(")).indent();
        q.addLine(trust("(${outbound}), (${inbound})")).outdent();
        q.addLine(trust(")"));
        q.addLine(trust("RETURN path"));
        return q.build().getValue();
    }

    @UnauthorizedAccess("Currently, this method is only applied to the internal database. Be cautious if sensitive information is going into the internal database and NEVER use it for other databases since this would be a vulnerability")
    public String getAllInternalDocumentsOfACollection(ArangoCollectionReference collection) {
        AQL q = new AQL();
        q.setParameter("collection", collection.getName());
        q.addLine(trust("FOR spec IN `${collection}` RETURN spec"));
        return q.build().getValue();
    }

    @UnauthorizedAccess("Currently, this method is only applied to the internal database. Be cautious if sensitive information is going into the internal database and NEVER use it for other databases since this would be a vulnerability")
    public String getInternalDocumentsOfCollectionWithKeyPrefix(ArangoCollectionReference collection, String keyPrefix) {
        AQL q = new AQL();
        q.setParameter("collection", collection.getName());
        q.setParameter("prefix", keyPrefix);
        q.addLine(trust("FOR spec IN `${collection}` "));
        q.addLine(trust("FILTER spec._key LIKE \"${prefix}%\""));
        q.addLine(trust("RETURN spec"));
        return q.build().getValue();
    }

    public String queryReleaseGraph(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference rootInstance, Integer maxDepth, Set<String> permissionGroupsWithReadAccess) {
        return childrenStatus(rootInstance, null, 0, maxDepth, edgeCollections, permissionGroupsWithReadAccess).getValue();
    }

    private TrustedAqlValue childrenStatus(ArangoDocumentReference rootInstance, String startingVertex, Integer level, Integer maxDepth, Set<ArangoCollectionReference> edgeCollections, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, level > 0);
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

        for (int i = 0; i < level; i++) {
            query.indent();
        }

        if (level == 0) {
            query.addLine(trust("LET ${name}_doc = DOCUMENT(\"${startId}\")"));
            query.addDocumentFilter(new TrustedAqlValue("${name}_doc"));
        } else {
            query.addLine(trust("FOR ${name}_doc, ${name}_edge IN 1..1 OUTBOUND ${doc} ${collections}"));
            query.addDocumentFilter(new TrustedAqlValue("${name}_doc"));
            query.addLine(trust("SORT ${name}_doc.`" + JsonLdConsts.TYPE + "`, ${name}_doc.`${schemaOrgName}`"));
        }
        query.addLine(trust("LET ${name}_release = (FOR ${name}_status_doc IN 1..1 INBOUND ${name}_doc `${releaseInstanceRelation}`"));
        query.indent();
        query.addLine(trust("LET ${name}_release_instance = SUBSTITUTE(CONCAT(${name}_status_doc.`${releaseInstanceProperty}`.`" + JsonLdConsts.ID + "`, \"?rev=\", ${name}_status_doc.`${releaseRevisionProperty}`), \"${nexusBaseForInstances}/\", \"\")"));
        query.addLine(trust("RETURN ${name}_release_instance==${name}_doc.${originalId} ? \"${releasedValue}\" : \"${changedValue}\""));
        query.outdent();
        query.addLine(trust(")"));
        query.addLine(trust("LET ${name}_status = LENGTH(${name}_release)>0 ? ${name}_release[0] : \"${notReleasedValue}\""));
        query.indent();
        query.addLine(trust("LET ${name}_children = ("));
        query.addLine(trust("${childrenQuery}"));
        query.addLine(trust(")"));
        query.outdent();
        if (level == 0) {
            query.addLine(trust("RETURN MERGE({\"status\": ${name}_status, \"children\": ${name}_children, \"rev\": ${name}_doc.`${revision}` }, ${name}_doc)"));
        } else {
            query.addLine(trust("RETURN MERGE({\"status\": ${name}_status, \"children\": ${name}_children, \"linkType\": ${name}_edge._name, \"rev\": ${name}_doc.`${revision}`}, ${name}_doc)"));
        }
        for (int i = 0; i < level; i++) {
            query.outdent();
        }
        return query.build();

    }

    public String getInstanceList(ArangoCollectionReference collection, Integer from, Integer size, String searchTerm, Set<String> permissionGroupsWithReadAccess, boolean sort) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess, false);
        query.setParameter("collection", collection.getName());
        query.setParameter("from", from != null ? from.toString() : null);
        query.setParameter("size", size != null ? size.toString() : null);
        boolean hasSearchTerm = searchTerm != null && !searchTerm.isEmpty();
        query.setParameter("searchTerm", hasSearchTerm ? searchTerm.toLowerCase() : null);
        query.setParameter("filterProperty", SchemaOrgVocabulary.NAME);

        query.addLine(trust("FOR doc IN `${collection}`"));
        query.addDocumentFilter(new TrustedAqlValue("doc"));
        if (hasSearchTerm) {
            query.addLine(trust("FILTER LIKE (LOWER(doc.`${filterProperty}`), \"%${searchTerm}%\")"));
        }
        if (sort) {
            query.addLine(trust("SORT doc.`${filterProperty}`"));
        }
        if (size != null) {
            if (from != null) {
                query.addLine(trust("LIMIT ${from}, ${size}"));
            } else {
                query.addLine(trust("LIMIT ${size}"));
            }
        }
        query.addLine(trust("RETURN doc"));
        return query.build().getValue();
    }


    public String getBookmarks(NexusInstanceReference doc, Integer from, Integer size, String searchTerm, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.setParameter("filterValue", doc.getFullId(false));
        boolean hasSearchTerm = searchTerm != null && !searchTerm.trim().isEmpty();
        q.setParameter("searchTerm", hasSearchTerm ? searchTerm.toLowerCase() : null);
        q.setParameter("from", from != null ? String.valueOf(from) : null);
        q.setParameter("size", size != null ? String.valueOf(size) : null);

        q.addLine(trust("FOR doc IN `hbpkg-core-bookmark-v0_0_1`")).indent();
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine(trust("FILTER CONTAINS(doc.`https://schema.hbp.eu/hbpkg/bookmarkList`.`https://schema.hbp.eu/relativeUrl`, \"${filterValue}\")")).indent();
        q.addLine(trust("LET instances = (")).indent();
        q.addLine(trust("FOR i IN 1..1 OUTBOUND doc `schema_hbp_eu-hbpkg-bookmarkInstanceLink`")).indent();
        q.addDocumentFilter(new TrustedAqlValue("i"));
        q.addLine(trust("FILTER i.`" + JsonLdConsts.ID + "` != NULL"));
        if (searchTerm != null && !searchTerm.isEmpty()) {
            q.addLine(trust("FILTER LIKE (LOWER(i.`" + SchemaOrgVocabulary.NAME + "`), \"%${searchTerm}%\")"));
        }
        q.addLine(trust("SORT i.`" + SchemaOrgVocabulary.NAME + "`"));
        if (from != null && size != null) {
            q.addLine(trust("LIMIT ${from}, ${size}"));
        }
        q.addLine(trust("RETURN {")).indent();
        q.addLine(trust("\"id\": i.`" + HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK + "`,"));
        q.addLine(trust("\"name\": i.`" + SchemaOrgVocabulary.NAME + "`,"));
        q.addLine(trust("\"dataType\": i.`" + JsonLdConsts.TYPE + "`,"));
        q.addLine(trust("\"description\": i.`" + SchemaOrgVocabulary.DESCRIPTION + "`"));
        q.addLine(trust("}")).outdent();
        q.addLine(trust(")")).outdent();
        q.addLine(trust("FILTER instances != null AND COUNT(instances) > 0")).outdent();
        q.addLine(trust("RETURN FIRST(instances)"));
        return q.build().getValue();
    }

    public String getAttributesWithCount(ArangoCollectionReference reference) {
        AQL q = new AQL();
        q.addLine(trust("FOR doc IN `${reference}`"));
        q.addLine(trust("FOR att IN ATTRIBUTES(doc, true)"));
        q.addLine(trust("COLLECT attribute = att WITH COUNT INTO numOfOccurences"));
        q.addLine(trust("RETURN { attribute, numOfOccurences }"));
        q.setParameter("reference", reference.getName());
        return q.build().getValue();
    }

    public String queryDirectRelationsWithType(ArangoCollectionReference reference, Set<ArangoCollectionReference> edgeCollections, boolean outbound) {
        AQL q = new AQL();
        q.setTrustedParameter("collections", q.listCollections(edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())));
        q.setParameter("fromOrTo", outbound ? ArangoVocabulary.TO : ArangoVocabulary.FROM);
        q.setParameter("reference", reference.getName());
        q.setParameter("direction", outbound ? "OUTBOUND" : "INBOUND");
        q.addLine(trust("FOR doc IN `${reference}`"));
        q.addLine(trust("FOR v, e IN 1..1 ${direction} doc ${collections}"));
        q.addDocumentFilter(new TrustedAqlValue("v"));
        q.addLine(trust("RETURN DISTINCT {"));
        q.indent().addLine(trust("\"ref\": SUBSTRING(e.${fromOrTo}, 0, FIND_LAST(e.${fromOrTo}, \"/\")), "));
        q.addLine(trust("\"attribute\": e._name"));
        q.addLine(trust("}"));
        return q.build().getValue();
    }


    public String queryOccurenceOfSchemasInRelation(ArangoCollectionReference originalCollection, ArangoCollectionReference relationCollection, Set<String> permissionGroupsWithReadAccess){
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        query.setParameter("type", originalCollection.getName());
        query.setParameter("relation", relationCollection.getName());
        query.addLine(trust("LET types = FLATTEN(FOR doc IN `${type}`"));
        query.addDocumentFilter(trust("doc"));
        query.addLine(trust("LET relation = (FOR r IN OUTBOUND doc `${relation}`"));
        query.indent().addDocumentFilter(trust("r"));
        query.addLine(trust("LET relativeUrl = SUBSTRING(r.`"+JsonLdConsts.ID+"`,  FIND_FIRST(r.`"+JsonLdConsts.ID+"`, \"/data/\")+6)"));
        query.addLine(trust("LET schema = SUBSTRING(relativeUrl, 0, FIND_LAST(relativeUrl, \"/\"))"));
        query.addLine(trust("RETURN schema"));
        query.addLine(trust(")"));
        query.addLine(trust("RETURN FLATTEN(relation)"));
        query.outdent().addLine(trust(")"));
        query.addLine(trust("FOR t IN types"));
        query.addLine(trust("COLLECT type = t WITH COUNT INTO length"));
        query.addLine(trust("SORT length DESC"));
        query.addLine(trust("RETURN {"));
        query.indent().addLine(trust("\"type\": type,"));
        query.addLine(trust("\"count\": length"));
        query.outdent().addLine(trust("}"));
        return query.build().getValue();
    }

    public String querySuggestionInstanceByUser(NexusInstanceReference instanceReference, NexusInstanceReference userRef, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        query.setParameter("documentId", ArangoNamingHelper.createCompatibleId(userRef.getNexusSchema().getRelativeUrl().getUrl()) + "/" + userRef.getId());
        query.addLine(trust("LET doc = DOCUMENT(\"${documentId}\")"));
        query.addLine(trust("FOR v IN 1..1 INBOUND doc `schema_hbp_eu-suggestion-user`"));
        query.addDocumentFilter(trust(("v")));
        query.addLine(trust("FILTER v.`" + HBPVocabulary.SUGGESTION_OF + "`.`" + HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK + "` ==\"" + instanceReference.getRelativeUrl().getUrl() + "\""));
        query.addLine(trust("FILTER v.`" + HBPVocabulary.SUGGESTION_STATUS + "` == \"" + SuggestionStatus.PENDING + "\""));
        query.addLine(trust("RETURN v"));
        return query.build().getValue();
    }

    public String querySuggestionsByUser(NexusInstanceReference ref, SuggestionStatus status, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        query.setParameter("documentId", ArangoNamingHelper.createCompatibleId(ref.getNexusSchema().getRelativeUrl().getUrl()) + "/" + ref.getId());
        query.addLine(trust("LET doc = DOCUMENT(\"${documentId}\")"));
        query.addLine(trust("FOR v IN 1..1 INBOUND doc `schema_hbp_eu-suggestion-user`"));
        query.addDocumentFilter(trust(("v")));
        query.addLine(trust("RETURN v`" + HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK + "`"));
        return query.build().getValue();
    }


    public String querySuggestionByField(ArangoCollectionReference originalCollection, ArangoCollectionReference relationCollection, String searchTerm, Integer start, Integer size, Set<String> permissionGroupsWithReadAccess, List<ArangoCollectionReference> types) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);

        query.setParameter("originCollection", originalCollection.getName());
        query.setParameter("relationCollection", relationCollection.getName());
        query.setParameter("nameField", SchemaOrgVocabulary.NAME);
        query.setParameter("idField", HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK);
        query.setParameter("searchTerm", searchTerm);
        query.setParameter("start", String.valueOf(start));
        query.setTrustedParameter("schemas", query.listCollections(types.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())));

        if (size != null) {
            query.setParameter("size", String.valueOf(size));
        }
        query.addLine(trust("LET relations = FLATTEN(FOR doc IN `${originCollection}`"));
        query.addDocumentFilter(trust("doc"));
        query.indent().addLine(trust("LET relation = (FOR r IN OUTBOUND doc `${relationCollection}`"));
        query.addDocumentFilter(trust("r"));
        if (searchTerm != null) {
            query.addLine(trust("&& like(r.`${nameField}`, \"%${searchTerm}%\", true)"));
        }
        query.addLine(trust("RETURN {"));
        query.indent().addLine(trust("\"id\":  r.`${idField}`,"));
        query.addLine(trust("\"name\": r.`${nameField}`"));
        query.addLine(trust("}"));
        query.outdent().addLine(trust(")"));
        query.addLine(trust("RETURN relation"));
        query.outdent().addLine(trust(")"));
        query.addLine(trust(""));
        query.addLine(trust("LET relationsPriorized = (FOR r IN relations"));
        query.indent().addLine(trust("COLLECT id = r.id, name=r.name WITH COUNT INTO num"));
        query.indent().addLine(trust("SORT num DESC"));
        query.addLine(trust("RETURN {"));
        query.indent().addLine(trust("id, name, num"));
        query.outdent().addLine(trust("})"));

        query.addLine(trust("LET schemas = [${schemas}]"));

        query.addLine(trust("LET fromSchemas = FLATTEN(FOR schema IN schemas"));
        query.addLine(trust("LET schemaInstance = (FOR i IN schema"));
        query.addDocumentFilter(trust("i"));
        if (searchTerm != null) {
            query.addLine(trust("&& like(i.`${nameField}`, \"%${searchTerm}%\", true)"));
        }
        query.addLine(trust("SORT i.`${nameField}` ASC"));
        query.addLine(trust("RETURN {"));
        query.indent().addLine(trust("\"id\":  i.`${idField}`,"));
        query.addLine(trust("\"name\": i.`${nameField}`"));
        query.addLine(trust("}"));
        query.outdent().addLine(trust(")"));
        query.addLine(trust("FILTER schemaInstance NOT IN relations"));
        query.addLine(trust("RETURN schemaInstance"));
        query.addLine(trust(")"));

        query.addLine(trust("LET result = APPEND(relationsPriorized, fromSchemas)"));
        query.addLine(trust("FOR r IN result"));
        if (size != null) {
            query.addLine(trust("LIMIT ${start}, ${size}"));
        }
        query.addLine(trust("RETURN r"));

        return query.build().getValue();
    }


    public String queryInstanceBySchemaAndFilter(ArangoCollectionReference collectionReference, String filterKey, String filterValue, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        query.setParameter("type", collectionReference.getName());
        query.addLine(trust("FOR doc IN `${type}`"));
        query.addDocumentFilter(new TrustedAqlValue(("doc")));
        query.indent().addLine(trust("FILTER doc.`" + filterKey + "` ==\"" + filterValue + "\""));
        query.addLine(trust("RETURN doc"));
        return query.build().getValue();
    }

    public String queryIncomingLinks(NexusInstanceReference ref, Set<ArangoCollectionReference> collections, Set<String> permissionGroupsWithReadAccess){
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        query.setTrustedParameter("collections", query.listCollections(collections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())));
        query.setParameter("documentId", ArangoNamingHelper.createCompatibleId(ref.getNexusSchema().getRelativeUrl().getUrl()) + "/" + ref.getId());
        query.setParameter("releaseCollection", ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE).getName());
        query.setParameter("releaseRevisionProperty", HBPVocabulary.RELEASE_REVISION)
                .setParameter("nexusBaseForInstances", configuration.getNexusBase(NexusConfiguration.ResourceType.DATA))
                .setParameter("originalId", ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV)
                .setParameter("releasedValue", ReleaseStatus.RELEASED.name())
                .setParameter("changedValue", ReleaseStatus.HAS_CHANGED.name())
                .setParameter("notReleasedValue", ReleaseStatus.NOT_RELEASED.name())
                .setParameter("releaseInstanceRelation", ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE).getName())
                .setParameter("releaseInstanceProperty", HBPVocabulary.RELEASE_INSTANCE);
        query.addLine(trust("LET doc = DOCUMENT(\"${documentId}\")"));
        query.addLine(trust("FOR v IN 1..1 INBOUND doc ${collections}"));
        query.indent();
        query.addLine(trust("LET release = (FOR rel IN 1..1 INBOUND v `${releaseInstanceRelation}`"));
        query.indent();
        query.addLine(trust("LET rel_instance = SUBSTITUTE(CONCAT(rel.`${releaseInstanceProperty}`.`" + JsonLdConsts.ID + "`, \"?rev=\", rel.`${releaseRevisionProperty}`), \"${nexusBaseForInstances}/\", \"\")"));
        query.addLine(trust("RETURN rel_instance==v.${originalId} ? \"${releasedValue}\" : \"${changedValue}\""));
        query.outdent();
        query.addLine(trust(")"));
        query.addLine(trust("LET status = LENGTH(release)>0 ? release[0] : \"${notReleasedValue}\""));
        query.addLine(trust("RETURN {\"doc\": v, \"status\":status}"));
        return query.build().getValue();
    }

    public String userReviewRequested(String userid, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
          query.setParameter("userid", userid)
                .setParameter("createdBy", HBPVocabulary.PROVENANCE_CREATED_BY)
                .setParameter("suggestionOf", HBPVocabulary.SUGGESTION_OF);
        query.addLine(trust("LET ids = (FOR doc IN `schema_hbp_eu-suggestion-suggestionOf` RETURN doc._from)"));
        query.addLine(trust("FOR id IN ids"));
        query.indent();
        query.addLine(trust("LET doc = DOCUMENT(id)"));
        query.addLine(trust("FILTER doc.`${createdBy}` == \"${userid}\" AND"));
        query.addLine(trust("doc.`${suggestionOf}` != null AND"));
        query.addLine(trust("doc.`${suggestionOf}` != \"\" AND"));
        query.addLine(trust("doc.`${suggestionOf}` != []"));
        query.outdent();
        query.addLine(trust("RETURN doc.`${suggestionOf}`.`@id`"));
        return  query.build().getValue();
    }

}
