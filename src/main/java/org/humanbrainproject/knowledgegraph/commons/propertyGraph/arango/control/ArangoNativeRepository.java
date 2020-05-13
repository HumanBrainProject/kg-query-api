/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.model.AqlQueryOptions;
import com.github.jsonldjava.core.JsonLdConsts;
import org.apache.commons.lang.StringUtils;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.UnexpectedNumberOfResults;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@ToBeTested(systemTestRequired = true)
public class ArangoNativeRepository {

    protected Logger logger = LoggerFactory.getLogger(ArangoNativeRepository.class);

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    ArangoRepository arangoRepository;

    public Set<NexusInstanceReference> findOriginalIdsWithLinkTo(ArangoConnection connection, ArangoDocumentReference instanceReference, ArangoCollectionReference collectionReference) {
        Set<ArangoCollectionReference> collections = connection.getCollections();
        if (collections.contains(instanceReference.getCollection()) && collections.contains(collectionReference)) {
            String query = queryFactory.queryOriginalIdForLink(instanceReference, collectionReference, authorizationContext.getReadableOrganizations());
            List<String> ids = connection.getOrCreateDB().query(query, null, new AqlQueryOptions(), String.class).asListRemaining();
            return ids.stream().filter(Objects::nonNull).map(NexusInstanceReference::createFromUrl).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * @return the reference to an instance based on the identifier value - this originates always from original space and the original id has to be resolved first, if it shall be used in another scope.
     */
    public NexusInstanceReference findBySchemaOrgIdentifier(ArangoCollectionReference collectionReference, String value) {
        if (!databaseFactory.getDefaultDB(false).getOrCreateDB().collection(collectionReference.getName()).exists()) {
            return null;
        }
        String query = queryFactory.queryForValueWithProperty(SchemaOrgVocabulary.IDENTIFIER, value, Collections.singleton(collectionReference), ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV, authorizationContext.getReadableOrganizations());
        List<List> result = query == null ? new ArrayList<>() : databaseFactory.getDefaultDB(false).getOrCreateDB().query(query, null, new AqlQueryOptions(), List.class).asListRemaining();
        if (result.size() == 1) {
            if (result.get(0) != null) {
                List list = (List) result.get(0);
                if (list.isEmpty()) {
                    return null;
                } else if (list.size() == 1) {
                    String url = (String) ((List) result.get(0)).get(0);
                    return url != null ? NexusInstanceReference.createFromUrl(url) : null;
                } else {
                    throw new UnexpectedNumberOfResults(String.format("Multiple instances with the same identifier in the same schema: %s", StringUtils.join(list.stream().filter(Objects::nonNull).map(Object::toString).toArray(), ", ")));
                }
            } else {
                return null;
            }
        }
        throw new UnexpectedNumberOfResults("The query for value with property should return a single item");
    }


    public Integer getCurrentRevision(ArangoDocumentReference documentReference) {
        Map document = arangoRepository.getDocument(documentReference, databaseFactory.getDefaultDB(false));
        if (document != null) {
            Object rev = document.get(ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV);
            if (rev != null) {
                String revStr = rev.toString().substring(rev.toString().indexOf("?rev=") + 5);
                return Integer.parseInt(revStr.trim());
            }
        }
        return null;
    }

    public JsonDocument getInstance(ArangoDocumentReference instanceReference) {
        return arangoRepository.getInstance(instanceReference, databaseFactory.getDefaultDB(false));
    }

    public Map getDocument(ArangoDocumentReference documentReference) {
        return arangoRepository.getDocument(documentReference, databaseFactory.getDefaultDB(false));
    }

    /**
     * @return the NexusInstanceReference of the original element (e.g. if we're requesting it with a reference of an instance extension, it will return the reference to the extended instance)
     */
    public NexusInstanceReference findOriginalId(NexusInstanceReference reference) {
        Map byKey = null;
        for (SubSpace subSpace : SubSpace.values()) {
            if (byKey == null) {
                ArangoDocumentReference arangoDocumentReferenceInSubSpace = ArangoDocumentReference.fromNexusInstance(reference.toSubSpace(subSpace));
                byKey = arangoRepository.getDocumentByKey(arangoDocumentReferenceInSubSpace, Map.class, databaseFactory.getDefaultDB(true));
            }
        }
        NexusInstanceReference result = reference.clone();
        if (byKey != null) {
            Object rev = byKey.get(ArangoVocabulary.NEXUS_REV);
            if (rev != null) {
                int revision = Integer.parseInt(rev.toString());
                if (result.getRevision() == null || result.getRevision() < revision) {
                    result.setRevision(revision);
                }
            }
            Object originalParent = byKey.get(HBPVocabulary.INFERENCE_EXTENDS);
            if (originalParent == null) {
                originalParent = byKey.get(HBPVocabulary.INFERENCE_OF);
            }
            NexusInstanceReference originalReference = null;
            if (originalParent instanceof Map) {
                String id = (String) ((Map) originalParent).get(JsonLdConsts.ID);
                originalReference = NexusInstanceReference.createFromUrl(id);
            } else if (byKey.get(JsonLdConsts.ID) != null) {
                originalReference = NexusInstanceReference.createFromUrl((String) byKey.get(JsonLdConsts.ID));
            }
            if (originalReference != null && !reference.isSameInstanceRegardlessOfRevision(originalReference)) {
                Map originalObject = arangoRepository.getDocumentByKey(ArangoDocumentReference.fromNexusInstance(originalReference), Map.class, databaseFactory.getDefaultDB(true));
                if (originalObject != null) {
                    Object originalRev = originalObject.get(ArangoVocabulary.NEXUS_REV);
                    if (originalRev != null) {
                        originalReference.setRevision(Integer.parseInt(originalRev.toString()));
                    }
                }
                result = originalReference;
            }
        }
        return result;
    }

}
