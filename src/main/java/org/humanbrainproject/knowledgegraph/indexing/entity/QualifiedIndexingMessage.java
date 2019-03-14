package org.humanbrainproject.knowledgegraph.indexing.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.*;

/**
 * This is a wrapper - the message we getInstance from Nexus contains non-resolved contexts. As a pre-processing step we fully qualify the
 * properties and store them in the corresponding map. This means, in the map you will find fully written keys (e.g. "http://schema.org/name" instead of "schema:name"
 *
 */
@ToBeTested(easy = true)
public class QualifiedIndexingMessage {
    private final Map qualifiedMap;
    private final IndexingMessage originalMessage;
    private final Set<String> types;


    public QualifiedIndexingMessage(IndexingMessage spec, Map qualifiedMap) {
        this.qualifiedMap = qualifiedMap;
        this.originalMessage = spec;
        Object type = this.qualifiedMap.get(JsonLdConsts.TYPE);
        if(type instanceof String){
            types = Collections.singleton((String)type);
        }
        else if(type instanceof Collection){
            HashSet<String> types = new HashSet<>();
            types.addAll((Collection<String>)type);
            this.types = Collections.unmodifiableSet(types);
        }
        else{
            types = Collections.emptySet();
        }
    }

    public Set<String> getTypes() {
        return types;
    }

    public boolean isOfType(String type){
        return types.contains(type);
    }

    public IndexingMessage getOriginalMessage() {
        return originalMessage;
    }

    public Map getQualifiedMap() {
        return qualifiedMap;
    }

    public Set<String> getIdentifiers(){
        Object identifier = this.qualifiedMap.get(SchemaOrgVocabulary.IDENTIFIER);
        if(identifier!=null) {
            if (identifier instanceof List) {
                return new HashSet<>((List) identifier);
            }
            return Collections.singleton(String.valueOf(identifier));
        }
        return Collections.emptySet();
    }


    public NexusInstanceReference getOriginalId(){
        Object originalParent = qualifiedMap.get(HBPVocabulary.INFERENCE_EXTENDS);
        if(originalParent==null){
            originalParent = qualifiedMap.get(HBPVocabulary.INFERENCE_OF);
        }
        if(originalParent==null){
            //The message neither points to an origin, nor to an inferred origin - it has to be the original itself.
            return originalMessage.getInstanceReference();
        }
        if (originalParent instanceof Map) {
          String id = (String) ((Map) originalParent).get(JsonLdConsts.ID);
          return NexusInstanceReference.createFromUrl(id);
        }
        return null;
    }

    public Integer getNexusRevision(){
        if(getOriginalMessage().getInstanceReference()!=null && getOriginalMessage().getInstanceReference().getRevision()!=null){
            return getOriginalMessage().getInstanceReference().getRevision();
        }
        Object o = getQualifiedMap().get(NexusVocabulary.REVISION_ALIAS);
        if(o==null){
            o = getQualifiedMap().get(ArangoVocabulary.NEXUS_REV);
        }
        if(o!=null) {
            Integer revision;
            try {
                revision = Integer.valueOf(o.toString());
            }
            catch(NumberFormatException e){
                revision = null;
            }
            return revision;
        }
        return null;
    }

}
