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

package org.humanbrainproject.knowledgegraph.suggestion.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInferredRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.suggestion.SuggestionStatus;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SuggestionController {

    @Autowired
    ArangoInferredRepository repository;

    @Autowired
    InstanceManipulationController instanceManipulationController;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;

    @Autowired
    QueryContext queryContext;

    @Autowired
    NexusConfiguration nexusConfiguration;

    public QueryResult<List<Map>> simpleSuggestByField(NexusSchemaReference originalSchema, String field, String type, String search, Pagination pagination){
        return repository.getSuggestionsByField(originalSchema, field, type, search, pagination);

    }

    public NexusInstanceReference createSuggestionInstanceForUser(NexusInstanceReference ref, String userId, String clientIdExtension) throws NotFoundException {
        Map m = this.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(m != null){
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> original = new HashMap<>();
            original.put(JsonLdConsts.ID,  nexusConfiguration.getAbsoluteUrl(ref));
            payload.put(HBPVocabulary.SUGGESTION_OF, original);
            payload.put(HBPVocabulary.SUGGESTION_USER_ID, userId);
            Map<String, Object> user = new HashMap<>();
            user.put(JsonLdConsts.ID, m.get(JsonLdConsts.ID));
            payload.put(HBPVocabulary.SUGGESTION_USER, user);
            payload.put(HBPVocabulary.SUGGESTION_STATUS, SuggestionStatus.PENDING);
            NexusSchemaReference schemaRef = ref.getNexusSchema();
            return instanceManipulationController.createNewInstance(schemaRef.toSubSpace(SubSpace.SUGGESTION), payload, clientIdExtension);
        }else{
            throw new NotFoundException("User not found");
        }
    }

    public Map getUserSuggestionOfSpecificInstance(NexusInstanceReference instanceReference, String userId) throws NotFoundException {
        Map m = this.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(m != null) {
            NexusInstanceReference userRef = NexusInstanceReference.createFromUrl((String) m.get(JsonLdConsts.ID));
            return repository.getUserSuggestionOfSpecificInstance(instanceReference, userRef);
        }else{
            throw new NotFoundException(("User not found"));
        }


    }

    private Map findInstanceBySchemaAndFilter(NexusSchemaReference schema, String filterKey, String filterValue){
        return repository.findInstanceBySchemaAndFilter(schema, filterKey, filterValue);
    }

    public List<Map> getUserSuggestions(String userId, SuggestionStatus status) throws NotFoundException{
        Map m = this.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(m != null){
            NexusInstanceReference ref = NexusInstanceReference.createFromUrl( (String) m.get(JsonLdConsts.ID));
            return repository.getSuggestionsByUser(ref, status);
        }else{
            throw new  NotFoundException(("User not found"));
        }
    }

    public JsonDocument changeSuggestionStatus(NexusInstanceReference ref, SuggestionStatus status, String clientIdExtension) throws NotFoundException{
        queryContext.setDatabaseScope(DatabaseScope.NATIVE);

        JsonDocument doc = arangoNativeRepository.getInstance(ArangoDocumentReference.fromNexusInstance(ref));
        if(doc != null){
            if(doc.get(HBPVocabulary.SUGGESTION_STATUS).equals(status.name())){
               throw new BadRequestException("Status already is " + status );
            }else{
                doc.put(HBPVocabulary.SUGGESTION_STATUS, status);
                doc.put(HBPVocabulary.SUGGESTION_STATUS_CHANGED_BY, clientIdExtension);
                return instanceManipulationController.directInstanceUpdate(ref, doc.getNexusRevision(), doc,  null);
            }
        }else{
            throw new NotFoundException("Instance not found");
        }
    }
}
