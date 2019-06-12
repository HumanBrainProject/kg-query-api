package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserInformation extends HashMap<String, Object> {

    public UserInformation(Map<? extends String, ?> map) {
        super(map);
    }

    public String getUserId(){
        return (String) get("sub");
    }

    public boolean hasCuratedPermission(){
        Object groups = get("groups");
        List<String> g = null;
        if(groups instanceof List){
            g = (List)groups;
        }
        else if(groups instanceof String){
            g = Arrays.asList(((String) groups).split(","));
        }
        return g!=null && g.contains("kg-curatedInstances");
    }

}
