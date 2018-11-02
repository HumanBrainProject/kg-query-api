package org.humanbrainproject.knowledgegraph.query.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;

import java.util.*;

public class JsonDocument extends LinkedHashMap<String, Object>{

    public void addReference(String propertyName, String url){
        Map<String, String> reference = new HashMap<>();
        reference.put(JsonLdConsts.ID, url);
        addToProperty(propertyName, reference);
    }

    public void addToProperty(String propertyName, Object value){
        addToProperty(propertyName, value, this);
    }

    public void addType(String type){
        addToProperty(JsonLdConsts.TYPE, type);
    }


    public void addAlternative(String propertyName, Object value){
        Map<String, Object> alternatives = (Map<String, Object>)get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        if(alternatives==null){
            alternatives = new LinkedHashMap<>();
            put(HBPVocabulary.INFERENCE_ALTERNATIVES, alternatives);
        }
        addToProperty(propertyName, value, alternatives);
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
            map.put(propertyName, Arrays.asList(o, value));
        }
    }

}
