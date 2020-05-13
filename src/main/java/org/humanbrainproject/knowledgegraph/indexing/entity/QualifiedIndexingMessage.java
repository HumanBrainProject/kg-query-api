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

package org.humanbrainproject.knowledgegraph.indexing.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
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
