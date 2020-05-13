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

package org.humanbrainproject.knowledgegraph.query.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.Alternative;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.AbsoluteNexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.*;
import java.util.function.Consumer;

@ToBeTested
public class JsonDocument extends LinkedHashMap<String, Object>{

    public JsonDocument(Map<? extends String, ?> map) {
        super(map);
    }

    public JsonDocument() {
    }

    public void addReference(String propertyName, String url){
        Map<String, String> reference = new HashMap<>();
        reference.put(JsonLdConsts.ID, url);
        addToProperty(propertyName, reference);
    }

    public JsonDocument addToProperty(String propertyName, Object value){
        addToProperty(propertyName, value, this);
        return this;
    }

    public boolean isOfType(String lookupType){
        Object type = get(JsonLdConsts.TYPE);
        if(type!=null && lookupType!=null){
            if(type instanceof String){
                return type.equals(lookupType);
            }
            else if(type instanceof Collection){
                return ((Collection)type).contains(lookupType);
            }
        }
        return false;
    }


    public void addType(String type){
        addToProperty(JsonLdConsts.TYPE, type);
    }

    public String getPrimaryIdentifier(){
        if(this.containsKey(SchemaOrgVocabulary.IDENTIFIER)){
            Object identifier = get(SchemaOrgVocabulary.IDENTIFIER);
            if(identifier instanceof List && !((List)identifier).isEmpty()){
                for (Object o : ((List) identifier)) {
                    if(o instanceof String){
                        return (String)o;
                    }
                }
            }
            else if(identifier instanceof String){
                return (String) identifier;
            }
        }
        return getNexusId();
    }

    public Integer getNexusRevision(){
        if(this.containsKey(NexusVocabulary.REVISION_ALIAS)){
            return (Integer)get(NexusVocabulary.REVISION_ALIAS);
        }
        else if(this.containsKey(ArangoVocabulary.NEXUS_REV)){
            Long rev = (Long) get(ArangoVocabulary.NEXUS_REV);
            return rev!=null ? rev.intValue() : null;
        }
        return null;
    }

    public String getNexusId(){
        return (String) get(ArangoVocabulary.NEXUS_UUID);
    }



    public NexusInstanceReference getReference(){
        if (this.containsKey(JsonLdConsts.ID)) {
            NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl((String) get(JsonLdConsts.ID));
            fromUrl.setRevision((Integer) get(NexusVocabulary.REVISION_ALIAS));
            return fromUrl;
        }
        return null;
    }


    public void addAlternative(String propertyName, Alternative value){
        Map<String, Object> alternatives = (Map<String, Object>)get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        if(alternatives==null){
            alternatives = new LinkedHashMap<>();
            put(HBPVocabulary.INFERENCE_ALTERNATIVES, alternatives);
        }
        Object v;
        if(alternatives.isEmpty()){
            v = new ArrayList<Alternative>();
            ((List) v).add(value);
        }else{
            v = value;
        }
        if(!value.getUserIds().isEmpty() && !value.getUserIds().stream().allMatch(Objects::isNull)){
            addToProperty(propertyName, v, alternatives);
        }
    }

    private static void addToProperty(String propertyName, Object value, Map map){
        Object o = map.get(propertyName);
        if(o==null){
            map.put(propertyName, value);
        }
        else if(o instanceof Collection){
            if(!((Collection)o).contains(value)) {
                ((Collection) o).add(value);
            }
        }
        else if(!o.equals(value)){
            List<Object> list = new ArrayList<>();
            list.add(o);
            list.add(value);
            map.put(propertyName, list);
        }
    }


    public JsonDocument removeAllInternalKeys(){
        this.keySet().removeIf(k -> k.startsWith("_"));
        return this;
    }


    public void processLinks(Consumer<Map> referenceConsumer){
        processLinks(referenceConsumer, this, true);
    }

    private void processLinks(Consumer<Map> referenceConsumer, Map currentMap, boolean root){
        //Skip root-id
        if(!root && currentMap.containsKey(JsonLdConsts.ID)){
            Object id = currentMap.get(JsonLdConsts.ID);
            if(id!=null){
                referenceConsumer.accept(currentMap);
            }
        }
        else {
            for (Object key : currentMap.keySet()) {
                Object value = currentMap.get(key);
                if(value instanceof Map){
                    processLinks(referenceConsumer, (Map)value, false);
                }
            }
        }
    }

    public void replaceNamespace(String oldNamespace, String newNamespace){
        replaceNamespace(oldNamespace, newNamespace, this);
    }

    private void replaceNamespace(String oldNamespace, String newNamespace, Map currentMap){
        HashSet keyList = new HashSet<>(currentMap.keySet());
        for (Object key : keyList) {
            if(key instanceof String){
                if(((String)key).startsWith(oldNamespace)){
                    Object value = currentMap.remove(key);
                    if(value instanceof Map){
                        replaceNamespace(oldNamespace, newNamespace, (Map)value);
                    }
                    currentMap.put(newNamespace+((String)key).substring(oldNamespace.length()), value);
                }
            }
        }
    }



}
