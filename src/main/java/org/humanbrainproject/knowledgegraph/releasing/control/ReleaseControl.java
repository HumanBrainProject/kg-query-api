package org.humanbrainproject.knowledgegraph.releasing.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.QueryParameters;
import org.humanbrainproject.knowledgegraph.query.entity.StoredQueryReference;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ReleaseControl {

    protected Logger logger = LoggerFactory.getLogger(ReleaseControl.class);

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;


    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    @Autowired
    ArangoQuery query;

    public NexusInstanceReference findNexusInstanceFromInferredArangoEntry(ArangoDocumentReference arangoDocumentReference, Credential credential) {
        Map document = arangoRepository.getDocument(arangoDocumentReference, databaseFactory.getInferredDB(), credential);
        if (document != null) {
            Object originalId = document.get(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV);
            if (originalId instanceof String) {
                return NexusInstanceReference.createFromUrl((String) originalId);
            }
        }
        return null;
    }

    public ReleaseStatusResponse getReleaseStatus(NexusInstanceReference instance, boolean withChildren, Credential credential) {
        Map releaseGraph = getReleaseGraph(instance, credential);
        if (releaseGraph != null) {
            ReleaseStatusResponse response = new ReleaseStatusResponse();
            response.setRootStatus(ReleaseStatus.valueOf((String) releaseGraph.get("status")));
            if (withChildren) {
                response.setChildrenStatus(findWorstReleaseStatusOfChildren(releaseGraph, null, true));
            }
            return response;
        }
        return null;
    }

    public Map getReleaseGraph(NexusInstanceReference instance, Credential credential) {
        try {
            QueryParameters parameters  = new QueryParameters(DatabaseScope.INFERRED, null);
            Map reflect = query.reflectQueryPropertyGraphByStoredSpecification(new StoredQueryReference(instance.getNexusSchema(), "search"), parameters, ArangoDocumentReference.fromNexusInstance(instance), credential);
            return reflect != null ? transformReleaseStatusMap(reflect) : null;
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }


    Map transformReleaseStatusMap(Map map) {
        Object name = map.get(SchemaOrgVocabulary.NAME);
        Object identifier = map.get(SchemaOrgVocabulary.IDENTIFIER);
        if (name != null) {
            map.put("label", name);
        } else {
            map.put("label", identifier);
        }
        if (map.containsKey(JsonLdConsts.TYPE)) {
            Object types = map.get(JsonLdConsts.TYPE);
            Object relevantType = null;
            if (types instanceof List && !((List) types).isEmpty()) {
                relevantType = ((List) types).get(0);
            } else {
                relevantType = types;
            }
            if (relevantType != null) {
                map.put("type", semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel(relevantType.toString()));
            }
        }
        if (map.containsKey(JsonLdConsts.ID)) {
            NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) map.get(JsonLdConsts.ID));
            if (fromUrl != null) {
                map.put("relativeUrl", fromUrl.getRelativeUrl().getUrl());
            }
        }

        Object linkType = map.get("linkType");
        if (linkType != null) {
            map.put("linkType", semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel((String) linkType));
        }

        for (Object key : map.keySet()) {
            if (map.get(key) instanceof Map) {
                transformReleaseStatusMap((Map) map.get(key));
            } else if (map.get(key) instanceof Collection) {
                for (Object o : ((Collection) map.get(key))) {
                    if (o instanceof Map) {
                        transformReleaseStatusMap((Map) o);
                    }
                }
            }
        }
        return map;

    }


    ReleaseStatus findWorstReleaseStatusOfChildren(Map map, ReleaseStatus currentStatus, boolean isRoot) {
        ReleaseStatus worstStatusSoFar = currentStatus;
        if (map != null) {
            //Skip status of root instance
            if (!isRoot) {
                try {
                    Object status = map.get("status");
                    if (status instanceof String) {
                        ReleaseStatus releaseStatus = ReleaseStatus.valueOf((String) status);
                        if (releaseStatus.isWorst()) {
                            return releaseStatus;
                        }
                        if (releaseStatus.isWorseThan(worstStatusSoFar)) {
                            worstStatusSoFar = releaseStatus;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    logger.error(String.format("Was not able to parse the status with the value %s", map.get("status")));
                }
            }

            Object children = map.get("children");
            if (children instanceof List) {
                for (Object o : ((List) children)) {
                    if (o instanceof Map) {
                        worstStatusSoFar = findWorstReleaseStatusOfChildren((Map) o, worstStatusSoFar, false);
                        if (worstStatusSoFar.isWorst()) {
                            return worstStatusSoFar;
                        }
                    }
                }
            }


        }
        return worstStatusSoFar;
    }


}
