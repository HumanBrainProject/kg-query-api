/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.releasing.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.StoredQuery;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ToBeTested(integrationTestRequired = true)
public class ReleaseControl {

    protected Logger logger = LoggerFactory.getLogger(ReleaseControl.class);

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    ArangoQuery query;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    ArangoNativeRepository nativeRepository;

    @Autowired
    Instances instances;

    @Autowired
    InstanceManipulationController instanceManipulationController;

    @Autowired
    AuthorizationContext authorizationContext;


    public NexusInstanceReference findNexusInstanceFromInferredArangoEntry(ArangoDocumentReference arangoDocumentReference) {
        Map document = arangoRepository.getDocument(arangoDocumentReference, databaseFactory.getInferredDB(false));
        if (document != null) {
            Object originalId = document.get(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV);
            if (originalId instanceof String) {
                return NexusInstanceReference.createFromUrl((String) originalId);
            }
        }
        return null;
    }

    public ReleaseStatusResponse getReleaseStatus(NexusInstanceReference instance, TreeScope scope) {
        Map releaseGraph = getReleaseGraph(instance, scope);
        if (releaseGraph != null) {
            ReleaseStatusResponse response = new ReleaseStatusResponse();
            if(!scope.name().equals(TreeScope.CHILDREN_ONLY.name())){
                response.setRootStatus(ReleaseStatus.valueOf((String) releaseGraph.get("status")));
            }
            if (!scope.name().equals(TreeScope.TOP_INSTANCE_ONLY.name())) {
                ReleaseStatus worstReleaseStatusOfChildren = findWorstReleaseStatusOfChildren(releaseGraph, null, true);
                response.setChildrenStatus(worstReleaseStatusOfChildren);
            }
            response.setId(instance);
            return response;
        }
        return null;
    }

    public Map getReleaseGraph(NexusInstanceReference instanceReference, TreeScope scope) {
        try {
            StoredQuery storedQuery = new StoredQuery(instanceReference.getNexusSchema(), "search", null);
            storedQuery.getFilter().restrictToSingleId(instanceReference.getId());
            Map releaseTree = query.queryReleaseTree(storedQuery, instanceReference, scope);
            return releaseTree != null ? transformReleaseStatusMap(releaseTree) : null;

        } catch (IOException | JSONException e) {
            logger.error("Was not able to request the release graph ", e);
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

        if (new JsonDocument(map).isOfType(HBPVocabulary.LINKING_INSTANCE_TYPE)) {
            map.put("isAssociation", true);
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

    public void release(NexusInstanceReference instanceReference) {
        NexusInstanceReference nexusInstanceFromInferredArangoEntry = findNexusInstanceFromInferredArangoEntry(ArangoDocumentReference.fromNexusInstance(instanceReference));
        release(nexusInstanceFromInferredArangoEntry, nexusInstanceFromInferredArangoEntry.getRevision());
    }

    public IndexingMessage release(NexusInstanceReference instanceReference, Integer revision) {
        JsonDocument payload = new JsonDocument();
        payload.addReference(HBPVocabulary.RELEASE_INSTANCE, configuration.getAbsoluteUrl(instanceReference));
        payload.put(HBPVocabulary.RELEASE_REVISION, revision);
        payload.addType(HBPVocabulary.RELEASE_TYPE);
        NexusInstanceReference originalId = nativeRepository.findOriginalId(instanceReference);
        NexusSchemaReference releaseSchema = new NexusSchemaReference("releasing", "prov", "release", "v0.0.2");
        Set<NexusInstanceReference> existingReleases = nativeRepository.findOriginalIdsWithLinkTo(databaseFactory.getInferredDB(false), ArangoDocumentReference.fromNexusInstance(originalId.toSubSpace(SubSpace.MAIN)), ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE));
        NexusInstanceReference instance = instanceManipulationController.createInstanceByIdentifier(releaseSchema, originalId.toSubSpace(SubSpace.MAIN).getFullId(false), payload, authorizationContext.getUserId());
        //TODO This is a progressive fix for a previously introduced issue. As soon as all release instances are containing an "identifier" with the main space, we can get rid of this logic.
        NexusInstanceReference finalInstance = instance;
        existingReleases.removeIf(instanceReference1 -> instanceReference1.isSameInstanceRegardlessOfRevision(instance));
        boolean outdatedRelease = false;
        for (NexusInstanceReference existingRelease : existingReleases) {
            outdatedRelease = true;
            logger.info("Outdated release: "+existingRelease);
            instanceManipulationController.deprecateInstanceByNexusId(existingRelease);
        }
        if(outdatedRelease){
            //The previous deprecations have temporarily unreleased the instance. This is why we need to re-release it.
            finalInstance = instanceManipulationController.createInstanceByIdentifier(releaseSchema, originalId.toSubSpace(SubSpace.MAIN).getFullId(false), payload, authorizationContext.getUserId());
        }
        return new IndexingMessage(finalInstance, jsonTransformer.getMapAsJson(payload), ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),  authorizationContext.getUserId());
    }

    public NexusInstanceReference unrelease(NexusInstanceReference instanceReference) {
        //We need the original id because the releasing mechanism needs to point to the real instance to ensure the right revision. We can do that by pointing to the nexus relative url of the inferred instance.
        Map document = arangoRepository.getDocument(ArangoDocumentReference.fromNexusInstance(instanceReference), databaseFactory.getInferredDB(false));
        if (document != null) {
            Object relativeUrl = document.get(ArangoVocabulary.NEXUS_RELATIVE_URL);
            if (relativeUrl != null) {
                NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) relativeUrl);
                NexusInstanceReference originalFrom = nativeRepository.findOriginalId(fromUrl);
                //Find release instance
                Set<NexusInstanceReference> releases = nativeRepository.findOriginalIdsWithLinkTo(databaseFactory.getInferredDB(false), ArangoDocumentReference.fromNexusInstance(originalFrom.toSubSpace(SubSpace.MAIN)), ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE));
                for (NexusInstanceReference nexusInstanceReference : releases) {
                    Integer currentRevision = nativeRepository.getCurrentRevision(ArangoDocumentReference.fromNexusInstance(nexusInstanceReference));
                    nexusInstanceReference.setRevision(currentRevision);
                    instanceManipulationController.deprecateInstanceByNexusId(nexusInstanceReference);
                }
                return fromUrl;
            }
        }
        return null;
    }

}
