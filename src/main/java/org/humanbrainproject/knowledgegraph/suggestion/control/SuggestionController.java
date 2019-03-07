package org.humanbrainproject.knowledgegraph.suggestion.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInferredRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.suggestion.SuggestionStatus;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.boundary.Instances;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceLookupController;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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
    ArangoInferredRepository arangoInferredRepository;

    @Autowired
    InstanceLookupController instanceLookupController;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    QueryContext queryContext;

    @Autowired
    NexusConfiguration nexusConfiguration;

    public QueryResult<List<Map>> simpleSuggestByField(NexusSchemaReference originalSchema, String field, String search, Pagination pagination){
        return repository.getSuggestionsByField(originalSchema, field, search, pagination);

    }

    public NexusInstanceReference createSuggestionInstanceForUser(NexusInstanceReference ref, String userId, String clientIdExtension) throws ResponseStatusException {
        List<Map> ms = this.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(ms != null){
            Map m = ms.get(0);
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found" );
        }
    }

    public Map getUserSuggestionOfSpecificInstance(NexusInstanceReference instanceReference, String userId) throws ResponseStatusException {
        List<Map> ms = this.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(ms != null) {
            Map m = ms.get(0);
            NexusInstanceReference userRef = NexusInstanceReference.createFromUrl((String) m.get(JsonLdConsts.ID));
            return repository.getUserSuggestionOfSpecificInstance(instanceReference, userRef);
        }else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found" );
        }


    }

    private List<Map> findInstanceBySchemaAndFilter(NexusSchemaReference schema, String filterKey, String filterValue){
        return repository.findInstanceBySchemaAndFilter(schema, filterKey, filterValue);
    }

    public List<String> getUserSuggestions(String userId, SuggestionStatus status) throws ResponseStatusException{
        List<Map> ms = this.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(ms != null){
            Map m = ms.get(0);
            NexusInstanceReference ref = NexusInstanceReference.createFromUrl( (String) m.get(JsonLdConsts.ID));
            return repository.getSuggestionsByUser(ref, status);
        }else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found" );
        }
    }

    public NexusInstanceReference changeSuggestionStatus(NexusInstanceReference ref, SuggestionStatus status, String clientIdExtension) throws ResponseStatusException{
        queryContext.setDatabaseScope(DatabaseScope.NATIVE);
        JsonDocument doc = arangoNativeRepository.getInstance(ArangoDocumentReference.fromNexusInstance(ref));
        if(doc != null){
            if(doc.get(HBPVocabulary.SUGGESTION_STATUS).equals(status.name())){
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status already is " + status );
            }else{
                doc.put(HBPVocabulary.SUGGESTION_STATUS, status);
                doc.put(HBPVocabulary.SUGGESTION_STATUS_CHANGED_BY, clientIdExtension);
                doc.removeAllInternalKeys();
                return instanceManipulationController.updateInstance(ref, doc, null);
            }
        }else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found" );
        }
    }

    public boolean deleteSuggestion(NexusInstanceReference ref) throws ResponseStatusException{
        queryContext.setDatabaseScope(DatabaseScope.INFERRED);
        JsonDocument doc = arangoRepository.getInstance(ArangoDocumentReference.fromNexusInstance(ref), queryContext.getDatabaseConnection());
        if(doc != null){
            return instanceManipulationController.removeInstance(ref);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found or already deprecated");
    }

    public List<String> getUserReviewRequested(String userId) {
        queryContext.setDatabaseScope(DatabaseScope.INFERRED);
        return arangoInferredRepository.getUserReviewRequested(userId);
    }

    public NexusInstanceReference updateInstance(NexusInstanceReference ref, Map modifications, String clientIdExtension) throws ResponseStatusException {
        JsonDocument update = new JsonDocument(modifications);
        queryContext.setDatabaseScope(DatabaseScope.INFERRED);
        JsonDocument doc = instanceLookupController.getInstance(ref);
        if(doc != null){
            if(doc.get(HBPVocabulary.SUGGESTION_STATUS).equals(SuggestionStatus.ACCEPTED) ||
                    doc.get(HBPVocabulary.SUGGESTION_STATUS).equals(SuggestionStatus.REJECTED)
            ){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is " + doc.get(HBPVocabulary.SUGGESTION_STATUS) );
            }else{
                update.put(HBPVocabulary.SUGGESTION_STATUS, SuggestionStatus.EDITED);
                update.put(HBPVocabulary.SUGGESTION_STATUS_CHANGED_BY, clientIdExtension);
                update.removeAllInternalKeys();
                return instanceManipulationController.updateInstance(ref, update,  null);
            }
        }else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found" );
        }
    }
}
