package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Tested
public class JsonTransformer {

    Gson gson = new Gson();

    public String getMapAsJson(Map map){
        return gson.toJson(map);
    }


    protected Logger logger = LoggerFactory.getLogger(JsonTransformer.class);

    /**
     * @return a single map if available. If the json string describes a (non-empty) list, the first item is returned. If the json is not a dict, null is returned
     */
    public Map parseToMap(String json) {
        Object o = gson.fromJson(json, Object.class);
        if(o instanceof Map){
            return (Map)o;
        }
        else if(o instanceof List && !((List)o).isEmpty()){
            Object firstElement = ((List) o).get(0);
            if(firstElement instanceof Map){
                return (Map)firstElement;
            }
        }
        return null;
    }

    /**
     * @return a list of strings (if the json follows the structure ["foo", "bar"] or null if it doesn't follow that structure
     */
    public List<String> parseToListOfStrings(String json){
        try {
            List list = gson.fromJson(json, List.class);
            return (List<String>) list.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
        }
        catch(JsonSyntaxException e){
            return null;
        }
    }

    public List<Map> parseToListOfMaps(String json){
        try{

            List list = gson.fromJson(json, List.class);
            if(list!=null){
                return (List<Map>)list.stream().filter(l -> l instanceof Map).map(l -> (Map) l).collect(Collectors.toList());
            }
            return null;
        }
        catch(JsonSyntaxException e){
            logger.error(String.format("Was not able to parse result in payload %s", json), e);
            return null;
        }
    }

    public String normalize(String json){
        return gson.toJson(gson.fromJson(json, Object.class));
    }

}
