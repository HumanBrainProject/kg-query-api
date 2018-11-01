package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ReferenceType;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    public String getDocument(ArangoDocumentReference document) {
//        return String.format("LET doc = DOCUMENT(\"%s\")\n" +
//                "RETURN doc", documentID);
        return String.format(
                "LET doc = DOCUMENT(\"%s\")" +
                        "LET status = (FOR status_doc IN 1..1 INBOUND doc `%s`\n" +
                        "FILTER  status_doc.`%s` != null \n" +
                        "RETURN DISTINCT status_doc.`%s`)\n" +
                        "RETURN MERGE({\"status\": status, \"rev\": doc.`%s` }, doc)"
                , document.getId(), ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE, ReferenceType.INTERNAL).getName(), HBPVocabulary.RELEASE_STATE, HBPVocabulary.RELEASE_STATE, HBPVocabulary.PROVENANCE_REVISION);
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


    public String queryDocumentWith1LevelOfEmbeddedInstances(ArangoDocumentReference document, Set<ArangoCollectionReference> arangoCollections) {
        Set<String> embeddedCollections = arangoCollections.stream().filter(c -> c.getName().startsWith(ReferenceType.EMBEDDED.getPrefix() + "-")).map(ArangoCollectionReference::getName).collect(Collectors.toSet());
        return String.format("LET doc = DOCUMENT(\"%s\")\n" +
                "LET embedded = (FOR vertex, edge IN 1..1 OUTBOUND doc `%s`\n" +
                "    COLLECT name = edge._name INTO verticesByEdgeName\n" +
                "    SORT verticesByEdgeName.edge._orderNumber\n" +
                "    RETURN {\n" +
                "        [ name ]: (FOR vertexByEdgeName IN verticesByEdgeName[*]\n" +
                "                RETURN vertexByEdgeName.vertex)\n" +
                "    })\n" +
                "\n" +
                "RETURN MERGE(APPEND([doc], embedded))", document.getId(), String.join("`, `", embeddedCollections));
    }


    public String queryReleaseGraph(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference rootInstance, Integer maxDepth, ArangoConnection driver) {
        Set<ArangoCollectionReference> collectionLabels = driver != null ? driver.filterExistingCollectionLabels(edgeCollections) : edgeCollections;
        String names = String.join("`, `", collectionLabels.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()));
        String start = String.format("DOCUMENT(\"%s\")", rootInstance.getId());
        return childrenStatus(start, 1, maxDepth, names);
    }

    private String childrenStatus(String startingVertex, Integer level, Integer maxDepth, String collectionLabels) {
        String name = "level" + level;
        String childrenQuery = "[]";
        if (level < maxDepth) {
            childrenQuery = String.format("(%s)", childrenStatus(name + "_doc", level + 1, maxDepth, collectionLabels));
        }

        return String.format("FOR %s_doc, %s_edge IN 1..1 OUTBOUND %s `%s`\n" +
                        "SORT %s_doc.`@type`, %s_doc.`http://schema.org/name`\n" +
                        "LET %s_status = (FOR %s_status_doc IN 1..1 INBOUND %s_doc `rel-hbp_eu-minds-releaseinstance`\n" +
                        "FILTER  %s_status_doc.`http://hbp.eu/minds#releasestate` != null \n" +
                        "RETURN DISTINCT %s_status_doc.`http://hbp.eu/minds#releasestate`)\n" +
                        "LET %s_children = %s\n" +
                        "RETURN MERGE({\"status\": %s_status, \"children\": %s_children, \"linkType\": %s_edge._id, \"rev\": %s_doc.`http://schema.hbp.eu/internal#rev`}, %s_doc)\n",
                name, name, startingVertex, collectionLabels, name, name, name, name, name, name, name, name, childrenQuery, name, name, name, name, name
        );
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
                "FOR doc IN `%s`\n" +
                        "%s" +
                        "SORT doc.`http://schema.org/name`, doc.`http://hbp.eu/minds#title`, doc.`http://hbp.eu/minds#alias`\n" +
                        "%s" +
                        "    RETURN doc", collection.getName(), collection, search, limit);
    }

    public String releaseStatus(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference documentReference, ArangoConnection driver) {
        //TODO still needed with release database?
//
//        Set<ArangoCollectionReference> collectionLabels= driver!=null ? driver.filterExistingCollectionLabels(edgeCollections) : edgeCollections;
//        Set<ArangoCollectionReference> collectionLabelsFiltered = collectionLabels.stream().filter( col -> !col.getTypeName().startsWith("rel-www_w3_org")).collect(Collectors.toSet());
//        String names = String.join("`, `", collectionLabelsFiltered.stream().map(ArangoCollectionReference::getTypeName).collect(Collectors.toSet()));
//        return String.format("" +
//                "LET doc = DOCUMENT(\"%s\")\n" +
//                "LET root_doc = doc._id != null? doc:DOCUMENT(\"%s\")\n" +
//                "LET status = (FOR status_doc IN 1..1 INBOUND root_doc `rel-hbp_eu-minds-releaseinstance`\n" +
//                "        RETURN DISTINCT status_doc.`http://hbp.eu/minds#releasestate`\n" +
//                "    )\n" +
//                "    LET child_status  =  ( \n" +
//                "        FOR level1_doc  IN 1..6 OUTBOUND root_doc `%s`\n" +
//                "            LET level1_status = ( \n" +
//                "                FOR level1_status_doc IN 1..1 INBOUND level1_doc `rel-hbp_eu-minds-releaseinstance`\n" +
//                "                    RETURN DISTINCT level1_status_doc.`http://hbp.eu/minds#releasestate`\n" +
//                "                )\n" +
//                "            LET child_s = \"released\" IN status? \"RELEASED\": \"NOT_RELEASED\"\n" +
//                "            RETURN child_s\n" +
//                "        )\n" +
//                "    LET s = \"released\" IN status? \"RELEASED\": \"NOT_RELEASED\"\n" +
//                "    return {\"status\":s, \"child_status\":child_status }",reconciledId, documentReference.getId(), names);
        return null;
    }


}
