package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.UnauthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.UnauthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        q.addLine("FOR doc in `${collection}`");
        q.addLine("FILTER doc._from == \"${fromId}\"");
        q.addLine("AND doc._to == \"${toId}\"");
        q.addLine("RETURN doc");
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
        q.addLine("LET doc = DOCUMENT(\"${documentId}\")");
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine("FOR v, e IN 1..1 ${direction} doc ${edges}");
        q.addDocumentFilter(new TrustedAqlValue("v"));
        q.addLine("RETURN e." + ArangoVocabulary.ID);
        return q.build().getValue();
    }

    public String queryOriginalIdForLink(ArangoDocumentReference document, ArangoCollectionReference linkReference, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.setParameter("documentId", document.getId());
        q.setParameter("edge", linkReference.getName());
        q.addLine("LET doc = DOCUMENT(\"${documentId}\") ");
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine("FOR v IN 1..1 INBOUND doc `${edge}`");
        q.addDocumentFilter(new TrustedAqlValue("v"));
        q.addLine("RETURN v." + ArangoVocabulary.NEXUS_RELATIVE_URL);

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
                subquery.addLine("FILTER v.`${propertyName}` == \"${propertyValue}\" OR \"${propertyValue}\" IN v.`${propertyName}` RETURN v.`${lookupProperty}`").outdent();
                subquery.outdent().addLine(")");
                q.addLine(subquery.build().getValue());
            }
            if (collectionsToCheck.size() > 1) {
                q.addLine("RETURN UNIQUE(UNION(${collections}))");
            } else {
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

    @UnauthorizedAccess("Currently, this method is only applied to the internal database. Be cautious if sensitive information is going into the internal database and NEVER use it for other databases since this would be a vulnerability")
    public String getInternalDocumentsOfCollectionWithKeyPrefix(ArangoCollectionReference collection, String keyPrefix) {
        UnauthorizedArangoQuery q = new UnauthorizedArangoQuery();
        q.setParameter("collection", collection.getName());
        q.setParameter("prefix", keyPrefix);
        q.addLine("FOR spec IN `${collection}` ");
        q.addLine("FILTER spec._key LIKE \"${prefix}%\"");
        q.addLine("RETURN spec");
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
        if (level == 0) {
            query.addLine("RETURN MERGE({\"status\": ${name}_status, \"children\": ${name}_children, \"rev\": ${name}_doc.`${revision}` }, ${name}_doc)");
        } else {
            query.addLine("RETURN MERGE({\"status\": ${name}_status, \"children\": ${name}_children, \"linkType\": ${name}_edge._name, \"rev\": ${name}_doc.`${revision}`}, ${name}_doc)");
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

        query.addLine("FOR doc IN `${collection}`");
        query.addDocumentFilter(new TrustedAqlValue("doc"));
        if (hasSearchTerm) {
            query.addLine("FILTER LIKE (LOWER(doc.`${filterProperty}`), \"%${searchTerm}%\")");
        }
        if (sort) {
            query.addLine("SORT doc.`${filterProperty}`");
        }
        if (size != null) {
            if (from != null) {
                query.addLine("LIMIT ${from}, ${size}");
            } else {
                query.addLine("LIMIT ${size}");
            }
        }
        query.addLine("RETURN doc");
        return query.build().getValue();
    }


    public String getBookmarks(NexusInstanceReference doc, Integer from, Integer size, String searchTerm, Set<String> permissionGroupsWithReadAccess) {
        AuthorizedArangoQuery q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        q.setParameter("filterValue", doc.getFullId(false));
        boolean hasSearchTerm = searchTerm != null && !searchTerm.trim().isEmpty();
        q.setParameter("searchTerm", hasSearchTerm ? searchTerm.toLowerCase() : null);
        q.setParameter("from", from != null ? String.valueOf(from) : null);
        q.setParameter("size", size != null ? String.valueOf(size) : null);

        q.addLine("FOR doc IN `hbpkg-core-bookmark-v0_0_1`").indent();
        q.addDocumentFilter(new TrustedAqlValue("doc"));
        q.addLine("FILTER CONTAINS(doc.`https://schema.hbp.eu/hbpkg/bookmarkList`.`https://schema.hbp.eu/relativeUrl`, \"${filterValue}\")").indent();
        q.addLine("LET instances = (").indent();
        q.addLine("FOR i IN 1..1 OUTBOUND doc `schema_hbp_eu-hbpkg-bookmarkInstanceLink`").indent();
        q.addDocumentFilter(new TrustedAqlValue("i"));
        q.addLine("FILTER i.`" + JsonLdConsts.ID + "` != NULL");
        if (searchTerm != null && !searchTerm.isEmpty()) {
            q.addLine("FILTER LIKE (LOWER(i.`" + SchemaOrgVocabulary.NAME + "`), \"%${searchTerm}%\")");
        }
        q.addLine("SORT i.`" + SchemaOrgVocabulary.NAME + "`");
        if (from != null && size != null) {
            q.addLine("LIMIT ${from}, ${size}");
        }
        q.addLine("RETURN {").indent();
        q.addLine("\"id\": i.`" + HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK + "`,");
        q.addLine("\"name\": i.`" + SchemaOrgVocabulary.NAME + "`,");
        q.addLine("\"dataType\": i.`" + JsonLdConsts.TYPE + "`,");
        q.addLine("\"description\": i.`" + SchemaOrgVocabulary.DESCRIPTION + "`");
        q.addLine("}").outdent();
        q.addLine(")").outdent();
        q.addLine("FILTER instances != null AND COUNT(instances) > 0").outdent();
        q.addLine("RETURN FIRST(instances)");
        return q.build().getValue();
    }

    public String getAttributesWithCount(ArangoCollectionReference reference) {
        UnauthorizedArangoQuery q = new UnauthorizedArangoQuery();
        q.addLine("FOR doc IN `${reference}`");
        q.addLine("FOR att IN ATTRIBUTES(doc, true)");
        q.addLine("COLLECT attribute = att WITH COUNT INTO numOfOccurences");
        q.addLine("RETURN { attribute, numOfOccurences }");
        q.setParameter("reference", reference.getName());
        return q.build().getValue();
    }

    public String queryDirectRelationsWithType(ArangoCollectionReference reference, Set<ArangoCollectionReference> edgeCollections, boolean outbound) {
        UnauthorizedArangoQuery q = new UnauthorizedArangoQuery();
        q.setTrustedParameter("collections", q.listCollections(edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())));
        q.setParameter("fromOrTo", outbound ? ArangoVocabulary.TO : ArangoVocabulary.FROM);
        q.setParameter("reference", reference.getName());
        q.setParameter("direction", outbound ? "OUTBOUND" : "INBOUND");
        q.addLine("FOR doc IN `${reference}`");
        q.addLine("FOR v, e IN 1..1 ${direction} doc ${collections}");
        q.addDocumentFilter(new TrustedAqlValue("v"));
        q.addLine("RETURN DISTINCT {");
        q.indent().addLine("\"ref\": SUBSTRING(e.${fromOrTo}, 0, FIND_LAST(e.${fromOrTo}, \"/\")), ");
        q.addLine("\"attribute\": e._name");
        q.addLine("}");
        return q.build().getValue();
    }


    public String queryOccurenceOfSchemasInRelation(ArangoCollectionReference originalCollection, ArangoCollectionReference relationCollection, Set<String> permissionGroupsWithReadAccess){
        AuthorizedArangoQuery query = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        query.setParameter("type", originalCollection.getName());
        query.setParameter("relation", relationCollection.getName());
        query.addLine("LET types = FLATTEN(FOR doc IN `${type}`");
        query.addDocumentFilter(new TrustedAqlValue("doc"));
        query.addLine("LET relation = (FOR r IN OUTBOUND doc `${relation}`");
        query.indent().addDocumentFilter(new TrustedAqlValue("r"));
        query.addLine("LET relativeUrl = SUBSTRING(r.`"+JsonLdConsts.ID+"`,  FIND_FIRST(r.`"+JsonLdConsts.ID+"`, \"/data/\")+6)");
        query.addLine("LET schema = SUBSTRING(relativeUrl, 0, FIND_LAST(relativeUrl, \"/\"))");
        query.addLine("RETURN schema");
        query.addLine(")");
        query.addLine("RETURN FLATTEN(relation)");
        query.outdent().addLine(")");
        query.addLine("FOR t IN types");
        query.addLine("COLLECT type = t WITH COUNT INTO length");
        query.addLine("SORT length DESC");
        query.addLine("RETURN {");
        query.indent().addLine("\"type\": type,");
        query.addLine("\"count\": length");
        query.outdent().addLine("}");
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
        query.addLine("LET relations = FLATTEN(FOR doc IN `${originCollection}`");
        query.addDocumentFilter(new TrustedAqlValue("doc"));
        query.indent().addLine("LET relation = (FOR r IN OUTBOUND doc `${relationCollection}`");
        query.addDocumentFilter(new TrustedAqlValue("r"));
        if (searchTerm != null) {
            query.addLine("&& like(r.`${nameField}`, \"%${searchTerm}%\", true)");
        }
        query.addLine("RETURN {");
        query.indent().addLine("\"id\":  r.`${idField}`,");
        query.addLine("\"name\": r.`${nameField}`");
        query.addLine("}");
        query.outdent().addLine(")");
        query.addLine("RETURN relation");
        query.outdent().addLine(")");
        query.addLine("");
        query.addLine("LET relationsPriorized = (FOR r IN relations");
        query.indent().addLine("COLLECT id = r.id, name=r.name WITH COUNT INTO num");
        query.indent().addLine("SORT num DESC");
        query.addLine("RETURN {");
        query.indent().addLine("id, name, num");
        query.outdent().addLine("})");

        query.addLine("LET schemas = [${schemas}]");

        query.addLine("LET fromSchemas = FLATTEN(FOR schema IN schemas");
        query.addLine("LET schemaInstance = (FOR i IN schema");
        query.addDocumentFilter(new TrustedAqlValue("i"));
        if (searchTerm != null) {
            query.addLine("&& like(i.`${nameField}`, \"%${searchTerm}%\", true)");
        }
        query.addLine("SORT i.`${nameField}` ASC");
        query.addLine("RETURN {");
        query.indent().addLine("\"id\":  i.`${idField}`,");
        query.addLine("\"name\": i.`${nameField}`");
        query.addLine("}");
        query.outdent().addLine(")");
        query.addLine("FILTER schemaInstance NOT IN relations");
        query.addLine("RETURN schemaInstance");
        query.addLine(")");

        query.addLine("LET result = APPEND(relationsPriorized, fromSchemas)");
        query.addLine("FOR r IN result");
        if (size != null) {
            query.addLine("LIMIT ${start}, ${size}");
        }
        query.addLine("RETURN r");

        return query.build().getValue();
    }





}
