package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ReferenceType;
import org.humanbrainproject.knowledgegraph.indexing.control.inference.InferenceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ArangoQueryFactory {

    @Autowired
    NexusConfiguration configuration;

    public String queryEdgesToBeRemoved(ArangoDocumentReference document, Set<ArangoCollectionReference> edgeCollections, Set<ArangoDocumentReference> excludeIds, ArangoConnection driver){
        Set<ArangoCollectionReference> collectionLabels=driver!=null ? driver.filterExistingCollectionLabels(edgeCollections) : edgeCollections;
        return String.format("LET doc = DOCUMENT(\"%s\")\n" +
                "    FOR v, e IN OUTBOUND doc `%s`\n" +
                "       FILTER e._id NOT IN [\"%s\"]\n" +
                "       return e._id", document.getId(), String.join("`, `", collectionLabels.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet())), String.join("\", \"", excludeIds.stream().map(ArangoDocumentReference::getId).collect(Collectors.toSet())));
    }


    public String queryForIdsWithProperty(String propertyName, String propertyValue, Set<ArangoCollectionReference> collectionsToCheck){
        StringBuilder sb = new StringBuilder();
        for (ArangoCollectionReference arangoCollectionReference : collectionsToCheck) {
            sb.append(String.format("LET `%s` = (FOR v IN `%s` FILTER v.`%s` == \"%s\" RETURN v._id)\n", arangoCollectionReference.getName(), arangoCollectionReference.getName(), propertyName, propertyValue));
        }
        sb.append(String.format("RETURN UNIQUE(UNION(`%s`))", String.join("`, `", collectionsToCheck.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()))));
        return sb.toString();
    }


//    public String queryAllEmbeddedInstancesForDocument(ArangoDocumentReference document){
//        return String.format("LET doc = DOCUMENT(\"%s\")\n" +
//                "    FOR v, e IN 1..10 OUTBOUND doc `%s`\n")
//
//
//    }


//    public String queryEdgeByFromAndTo(String edgeLabel, String from, String to){
//        return String.format("FOR rel IN `%s` FILTER rel._from==\"%s\" AND rel._to==\"%s\" RETURN rel", edgeLabel, from, to);
//    }

    public String queryPropertyCount(ArangoCollectionReference collection) {
        return String.format("LET attributesPerDocument = ( FOR doc IN `%s` RETURN ATTRIBUTES(doc, true) )\n" +
                "FOR attributeArray IN attributesPerDocument\n" +
                "    FOR attribute IN attributeArray\n" +
                "        COLLECT attr = attribute WITH COUNT INTO count\n" +
                "        SORT count DESC\n" +
                "        RETURN {attr, count}", collection.getName());
    }

    public String queryArangoNameMappings(ArangoCollectionReference lookupCollection){
        return String.format("FOR doc IN `%s` RETURN {\"arango\": doc._key, \"original\": doc.originalName}", lookupCollection.getName());
    }


    public String getAll(ArangoCollectionReference collection){
        return String.format("FOR doc IN `%s` RETURN doc", collection.getName());
    }

//    public String createEmbeddedInstancesQuery(Set<ArangoCollectionReference> edgeCollectionNames, String id, ArangoConnection driver) {
//        Set<String> collectionLabels= driver!=null ? driver.filterExistingCollectionLabels(edgeCollectionNames) : edgeCollectionNames;
//        String names = String.join("`, `", collectionLabels);
//        return String.format("FOR v, e IN 1..1 OUTBOUND \"%s\" `%s` \n" +
//                "        \n" +
//                "        return {\"vertexId\":v._id, \"edgeId\": e._id, \"isEmbedded\": v.`%s`==true}", id, names, configuration.getEmbedded());
//    }

    public String queryInDepthGraph(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference startDocument, Integer step, ArangoConnection driver) {
        Set<ArangoCollectionReference> collectionLabels= driver!=null ? driver.filterExistingCollectionLabels(edgeCollections) : edgeCollections;
        String names = String.join("`, `", collectionLabels.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()));
        String outbound = String.format("" +
                "FOR v, e, p IN 1..%s OUTBOUND \"%s\" `%s` \n" +
                "FILTER v.`http://schema.hbp.eu/internal#permissionGroup` IN whitelist_organizations \n " +
                "        return p",step, startDocument.getId(), names);
        String inbound = String.format("" +
                "FOR v, e, p IN 1..1 INBOUND \"%s\" `%s` \n" +
                "FILTER v.`http://schema.hbp.eu/internal#permissionGroup` IN whitelist_organizations \n " +
                "        return p", startDocument.getId(), names);
        return String.format("" +
                "LET whitelist_organizations=[\"minds\",\"brainviewer\",\"cscs\",\"datacite\",\"licenses\",\"minds2\",\"neuroglancer\"]" +
                "FOR path IN UNION_DISTINCT(" +
                "(%s),(%s)" +
                ")" +
                "return path", outbound, inbound);
    }

    public String getDocument(ArangoDocumentReference document){
//        return String.format("LET doc = DOCUMENT(\"%s\")\n" +
//                "RETURN doc", documentID);
        return String.format(
                "LET doc = DOCUMENT(\"%s\")" +
                        "LET status = (FOR status_doc IN 1..1 INBOUND doc `rel-hbp_eu-minds-releaseinstance`\n" +
                        "FILTER  status_doc.`http://hbp.eu/minds#releasestate` != null \n" +
                        "RETURN DISTINCT status_doc.`http://hbp.eu/minds#releasestate`)\n" +
                        "RETURN MERGE({\"status\": status, \"rev\": doc.`http://schema.hbp.eu/internal#rev` }, doc)"
        , document.getId());
    }

    public String getGetEditorSpecDocument(ArangoCollectionReference collection){
        return String.format(
                "FOR spec IN `%s`" +
                "RETURN spec", collection.getName()
        );
    }

    public String queryOriginalIdForLink(ArangoDocumentReference document, ArangoCollectionReference linkReference){
        return String.format("FOR vertex IN 1..1 INBOUND DOCUMENT(\"%s\") `%s` RETURN vertex._originalId", document.getId(), linkReference.getName());
    }


    public String queryDocumentWith1LevelOfEmbeddedInstances(ArangoDocumentReference document, Set<ArangoCollectionReference> arangoCollections){
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
        Set<ArangoCollectionReference> collectionLabels= driver!=null ? driver.filterExistingCollectionLabels(edgeCollections) : edgeCollections;
        Set<ArangoCollectionReference> collectionLabelsFiltered = collectionLabels.stream().filter( col ->
                !col.getName().startsWith("rel-www_w3_org") &&
                !col.getName().startsWith("rel-hbp_eu-reconciled-original_parent") &&
                !col.getName().startsWith("rel-hbp_eu-reconciled-alternatives") &&
                !col.getName().startsWith("rel-hbp_eu-reconciled-origin") &&
                !col.getName().startsWith("rel-hbp_eu-reconciled-parents")
        ).collect(Collectors.toSet());
        String names = String.join("`, `", collectionLabels.stream().map(ArangoCollectionReference::getName).collect(Collectors.toSet()));
        String start = String.format("DOCUMENT(\"%s\")", rootInstance.getId());
        return  childrenStatus(start, 1, maxDepth, names);
    }

    private String childrenStatus(String startingVertex, Integer level, Integer maxDepth, String collectionLabels){
        String name = "level"+level;
        String childrenQuery = "[]";
        if(level < maxDepth){
            childrenQuery = String.format("(%s)", childrenStatus(name+"_doc", level+ 1, maxDepth, collectionLabels));
        }

        return String.format("FOR %s_doc, %s_edge IN 1..1 OUTBOUND %s `%s`\n" +
                "SORT %s_doc.`@type`, %s_doc.`http://schema.org/name`\n" +
                "LET %s_status = (FOR %s_status_doc IN 1..1 INBOUND %s_doc `rel-hbp_eu-minds-releaseinstance`\n" +
                "FILTER  %s_status_doc.`http://hbp.eu/minds#releasestate` != null \n" +
                "RETURN DISTINCT %s_status_doc.`http://hbp.eu/minds#releasestate`)\n" +
                "LET %s_children = %s\n" +
                "RETURN MERGE({\"status\": %s_status, \"children\": %s_children, \"linkType\": %s_edge._id, \"rev\": %s_doc.`http://schema.hbp.eu/internal#rev`}, %s_doc)\n",
                name, name, startingVertex, collectionLabels,name, name, name, name, name, name, name, name, childrenQuery, name, name,name, name, name
        );
    }

    public String getInstanceList(String collection,Integer from, Integer size,String searchTerm, String recCollection){
        String search = "";
        if(searchTerm != null && !searchTerm.isEmpty()){
            searchTerm = searchTerm.toLowerCase();
            search = String.format("FILTER LIKE (LOWER(el.`http://schema.org/name`), \"%%%s%%\")\n", searchTerm);
        }
        String limit = "";
        if(from != null && size != null){
            limit =  String.format("LIMIT %s, %s \n", from.toString(), size.toString());
        }
        return String.format("LET rec = (FOR rec_doc IN %s\n" +
                "    RETURN rec_doc)\n" +
                "LET f = (FOR e IN rec\n" +
                "    RETURN SPLIT( e.`http://hbp.eu/reconciled#origin`, \"v0/data/\")[1]\n" +
                ")\n" +
                "LET minds = ( FOR doc IN `%s`\n" +
                "    FILTER (doc.`@id` NOT IN f)\n" +
                "    RETURN doc\n" +
                ")\n" +
                "FOR el IN UNION(minds, rec)\n" +
                "%s" +
                "SORT el.`http://schema.org/name`, el.`http://hbp.eu/minds#title`, el.`http://hbp.eu/minds#alias`\n" +
                "%s" +
                "    RETURN el", recCollection, collection, search, limit);
    }

    public String releaseStatus(Set<ArangoCollectionReference> edgeCollections, ArangoDocumentReference documentReference, ArangoConnection driver){
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


    public String getReconciledEntityGroup(String reconciledSpace, String relativeSourceId){
        return String.format("FOR doc IN `%s` FILTER \n" +
                "    \"%s\" == doc.`%s`.`%s` OR \n" +
                "    \"%s\" IN doc.`%s`[*].`%s`\n" +
                "    RETURN { \"reconciled\": doc.`%s`, \"sources\": UNION([doc.`%s`.`%s`], doc.`%s`[*].`%s`)}", reconciledSpace, relativeSourceId, InferenceController.INFERRED_SOURCE, JsonLdConsts.ID, relativeSourceId, InferenceController.INFERRED_SOURCE, JsonLdConsts.ID, JsonLdConsts.ID, InferenceController.INFERRED_SOURCE, JsonLdConsts.ID, InferenceController.INFERRED_SOURCE, JsonLdConsts.ID);
    }


}
