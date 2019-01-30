package org.humanbrainproject.knowledgegraph.suggestion.boundary;

import com.github.jsonldjava.core.JsonLdConsts;
import javassist.NotFoundException;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceLookupController;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.suggestion.control.SuggestionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Suggest {

    @Autowired
    SuggestionController suggestionController;

    @Autowired
    InstanceManipulationController instanceManipulationController;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;

    @Autowired
    NexusConfiguration nexusConfiguration;

    public QueryResult<List<Map>> suggestByField(NexusSchemaReference schemaReference, String field, String search, Pagination pagination){
        return suggestionController.simpleSuggestByField(schemaReference, field, search, pagination);
    }

    public NexusInstanceReference createSuggestionInstanceForUser(NexusInstanceReference ref, String userId, String clientIdExtension) throws Exception{
        NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(ref);
        Map m = suggestionController.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(m != null){
            Map<String, Object> payload = new HashMap<>();
            payload.put(HBPVocabulary.SUGGESTION_OF, ref.getRelativeUrl().getUrl());
            payload.put(HBPVocabulary.SUGGESTION_OF_ORIGINAL, originalId.getRelativeUrl().getUrl());
            payload.put(HBPVocabulary.SUGGESTION_USER_ID, userId);
            Map<String, Object> user = new HashMap<>();
            user.put(JsonLdConsts.ID, m.get(JsonLdConsts.ID));
            payload.put(HBPVocabulary.SUGGESTION_USER, user);
            NexusSchemaReference schemaRef = ref.getNexusSchema();
            return instanceManipulationController.createNewInstance(schemaRef.toSubSpace(SubSpace.SUGGESTION), payload, clientIdExtension);
        }else{
            throw new Exception("User not found");
        }
    }

    public Map getUserSuggestionOfSpecificInstance(NexusInstanceReference ref, String userId) throws NotFoundException {
        Map m = suggestionController.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(m != null) {
            NexusInstanceReference userRef = NexusInstanceReference.createFromUrl((String) m.get(JsonLdConsts.ID));
            return suggestionController.getUserSuggestionOfSpecificInstance(ref, userRef);
        }else{
            throw new NotFoundException(("User not found"));
        }
    }

    public List<Map> getUserSuggestions(String userId) throws NotFoundException{
        Map m = suggestionController.findInstanceBySchemaAndFilter(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), "https://schema.hbp.eu/hbpkg/userId", userId);
        if(m != null){
            NexusInstanceReference ref = NexusInstanceReference.createFromUrl( (String) m.get(JsonLdConsts.ID));
            return suggestionController.getUserSuggestions(ref);
        }else{
            throw new NotFoundException(("User not found"));
        }
    }


}
