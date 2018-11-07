package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.commons.text.StringSubstitutor;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ArangoQueryFactory {


    @Autowired
    NexusConfiguration configuration;

    public String queryForIdsWithProperty(String propertyName, String propertyValue, Set<ArangoCollectionReference> collectionsToCheck) {
        if (collectionsToCheck != null && !collectionsToCheck.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ArangoCollectionReference arangoCollectionReference : collectionsToCheck) {
                sb.append(String.format("LET `%s` = (FOR v IN `%s` FILTER v.`%s` == \"%s\" RETURN v._id)\n", arangoCollectionReference.getName(), arangoCollectionReference.getName(), propertyName, propertyValue));
            }
            sb.append(String.format("RETURN UNIQUE(UNION(`%s`))", String.join("`, `", collectionsToCheck.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()))));
            return sb.toString();
        }
        return null;
    }

    public String queryPropertyCount(ArangoCollectionReference collection) {
        return String.format("LET attributesPerDocument = ( FOR doc IN `%s` RETURN ATTRIBUTES(doc, true) )\n" +
                "FOR attributeArray IN attributesPerDocument\n" +
                "    FOR attribute IN attributeArray\n" +
                "        COLLECT attr = attribute WITH COUNT INTO count\n" +
                "        SORT count DESC\n" +
                "        RETURN {attr, count}", collection.getName());
    }

    public String queryArangoNameMappings(ArangoCollectionReference lookupCollection) {
        return String.format("FOR doc IN `%s` RETURN {\"arango\": doc._key, \"original\": doc.originalName}", lookupCollection.getName());
    }

    public String getAll(ArangoCollectionReference collection) {
        return String.format("FOR doc IN `%s` RETURN doc", collection.getName());
    }

    public String queryInDepthGraph(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference startDocument, Integer step, ArangoConnection driver) {
        Set<ArangoCollectionReference> collectionLabels = driver != null ? driver.filterExistingCollectionLabels(edgeCollections) : edgeCollections;
        String names = String.join("`, `", collectionLabels.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()));
        String outbound = String.format("" +
                "FOR v, e, p IN 1..%s OUTBOUND \"%s\" `%s` \n" +
                "FILTER v.`_permissionGroup` IN whitelist_organizations \n " +
                "        return p", step, startDocument.getId(), names);
        String inbound = String.format("" +
                "FOR v, e, p IN 1..1 INBOUND \"%s\" `%s` \n" +
                "FILTER v.`_permissionGroup` IN whitelist_organizations \n " +
                "        return p", startDocument.getId(), names);
        return String.format("" +
                "LET whitelist_organizations=[\"minds\",\"brainviewer\",\"cscs\",\"datacite\",\"licenses\",\"minds2\",\"neuroglancer\"]" +
                "FOR path IN UNION_DISTINCT(" +
                "(%s),(%s)" +
                ")" +
                "return path", outbound, inbound);
    }

    public String getGetEditorSpecDocument(ArangoCollectionReference collection) {
        return String.format(
                "FOR spec IN `%s`" +
                        "RETURN spec", collection.getName()
        );
    }

    public String queryOriginalIdForLink(ArangoDocumentReference document, ArangoCollectionReference linkReference) {
        return String.format("FOR vertex IN 1..1 INBOUND DOCUMENT(\"%s\") `%s` RETURN vertex._originalId", document.getId(), linkReference.getName());
    }


    public String queryOutboundRelationsForDocument(ArangoDocumentReference document, Set<ArangoCollectionReference> edgeCollections) {
        return String.format("FOR rel, edge IN 1..1 OUTBOUND DOCUMENT(\"%s\") `%s` RETURN edge._id\n", document.getId(), String.join("`, `", edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())));
    }

    public String queryReleaseGraph(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference rootInstance, Integer maxDepth) {
        String names = String.join("`, `", edgeCollections.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()));
        return childrenStatus(rootInstance, null, 0, maxDepth, names);
    }


    private String createIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("   ");
        }
        return sb.toString();
    }

    private String childrenStatus(ArangoDocumentReference rootInstance, String startingVertex, Integer level, Integer maxDepth, String collectionLabels) {
        String name = "level" + level;
        String childrenQuery = "[]";
        if (level < maxDepth) {
            childrenQuery = String.format("(%s)", childrenStatus(rootInstance, name + "_doc", level + 1, maxDepth, collectionLabels));
        }

        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("name", "level" + level);
        valueMap.put("startId", rootInstance.getId());
        valueMap.put("doc", startingVertex);
        valueMap.put("collections", collectionLabels);
        valueMap.put("releaseInstanceRelation", ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE).getName());
        valueMap.put("releaseState", HBPVocabulary.RELEASE_STATE);
        valueMap.put("revision", HBPVocabulary.PROVENANCE_REVISION);
        valueMap.put("childrenQuery", childrenQuery);
        valueMap.put("schemaOrgName", SchemaOrgVocabulary.NAME);
        valueMap.put("releaseInstanceProperty", HBPVocabulary.RELEASE_INSTANCE);
        valueMap.put("releaseRevisionProperty", HBPVocabulary.RELEASE_REVISION);
        valueMap.put("nexusBaseForInstances", configuration.getNexusBase(NexusConfiguration.ResourceType.DATA));
        valueMap.put("originalId", "_originalId");
        valueMap.put("releasedValue", ReleaseStatus.RELEASED.name());
        valueMap.put("changedValue", ReleaseStatus.HAS_CHANGED.name());
        valueMap.put("notReleasedValue", ReleaseStatus.NOT_RELEASED.name());

        String indent = createIndent(level);
        String query = "";
        if (level == 0) {
            query += indent + "LET ${name}_doc = DOCUMENT(\"${startId}\")\n ";
        } else {
            query += indent + "FOR ${name}_doc, ${name}_edge IN 1..1 OUTBOUND ${doc} `${collections}`\n " +
                    indent + "SORT ${name}_doc.`" + JsonLdConsts.TYPE + "`, ${name}_doc.`${schemaOrgName}`\n";
        }
        query += indent + "LET ${name}_release = (FOR ${name}_status_doc IN 1..1 INBOUND ${name}_doc `${releaseInstanceRelation}`\n" +
                indent + "  LET ${name}_release_instance = SUBSTITUTE(CONCAT(${name}_status_doc.`${releaseInstanceProperty}`.`" + JsonLdConsts.ID + "`, \"?rev=\", ${name}_status_doc.`${releaseRevisionProperty}`), \"${nexusBaseForInstances}/\", \"\")\n" +
                indent + "  RETURN ${name}_release_instance==${name}_doc.${originalId} ? \"${releasedValue}\" : \"${changedValue}\"\n" +
                indent + ")\n" +
                indent + "LET ${name}_status = LENGTH(${name}_release)>0 ? ${name}_release[0] : \"${notReleasedValue}\"\n" +
                indent + "LET ${name}_children = ${childrenQuery}\n";
        if (level == 0) {
            query += indent + "RETURN MERGE({\"status\": ${name}_status, \"children\": ${name}_children, \"rev\": ${name}_doc.`${revision}` }, ${name}_doc)";
        } else {
            query += indent + "RETURN MERGE({\"status\": ${name}_status, \"children\": ${name}_children, \"linkType\": ${name}_edge._name, \"rev\": ${name}_doc.`${revision}`}, ${name}_doc)\n";
        }
        return StringSubstitutor.replace(query, valueMap);

    }

    public String getInstanceList(ArangoCollectionReference collection, Integer from, Integer size, String searchTerm) {
        String search = "";
        if (searchTerm != null && !searchTerm.isEmpty()) {
            searchTerm = searchTerm.toLowerCase();
            search = String.format("FILTER LIKE (LOWER(doc.`http://schema.org/name`), \"%%%s%%\")\n", searchTerm);
        }
        String limit = "";
        if (from != null && size != null) {
            limit = String.format("LIMIT %s, %s \n", from.toString(), size.toString());
        }
        return String.format(
                "FOR doc IN `%s` \n" +
                        "%s " +
                        "SORT doc.`http://schema.org/name`, doc.`http://hbp.eu/minds#title`, doc.`http://hbp.eu/minds#alias` \n" +
                        "%s " +
                        "    RETURN doc", collection.getName(), search, limit);
    }

    public String getOriginalIdOfDocumentWithChildren(ArangoDocumentReference documentReference, ArangoConnection connection) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(String.format("LET doc = DOCUMENT(\"%s\")\n", documentReference.getId()));
        Set<ArangoCollectionReference> edgesCollectionNames = connection.getEdgesCollectionNames();
        if (!edgesCollectionNames.isEmpty()) {
            String names = String.join("`, `", edgesCollectionNames.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()));
            queryBuilder.append(String.format("LET children = (FOR child IN 1..6 OUTBOUND doc `%s` return child._originalId ) \n", names));
        } else {
            queryBuilder.append("LET children = [] \n");
        }
        queryBuilder.append("RETURN {\"root\": doc._originalId, \"children\": children}");
        return queryBuilder.toString();
    }

    public String getInstance(ArangoDocumentReference ref) {
        return String.format(
                "LET doc = DOCUMENT(\"%s\") \n" +
                        "RETURN doc",
                ref.getId()
        );
    }


    public String getOriginalIds(ArangoCollectionReference collectionReference, Set<String> keys, ArangoConnection connection) {
        return String.format("FOR doc IN `%s`\n" +
                "FILTER doc._key IN [\"%s\"]\n" +
                "RETURN doc._originalId\n" +
                "\n", collectionReference.getName(), String.join("\", \"", keys));
    }
}
