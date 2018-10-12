package org.humanbrainproject.knowledgegraph.entity.query;

import java.util.HashMap;
import java.util.Map;

public class ArangoBindVariable {
    private Map<String, Object> bindVariables = new HashMap<>();

    public String put(String name, Object val, Integer occurrence){
        if(occurrence ==  null){
            occurrence = 0;
        }
        String genName = name.concat(occurrence.toString());
        Object o = this.bindVariables.get(genName);
        if(o == null){
            this.bindVariables.put(genName, val);
        }else{
            if(!o.equals(val)){
                return this.put(name, val, occurrence + 1);
            }
        }
        return genName;
    }

    public Object get(String name){
        return this.bindVariables.get(name);
    }

    public Map<String, Object> extractMap(){
        return new HashMap<>(this.bindVariables);
    }
}
