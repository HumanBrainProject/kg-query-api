package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import java.util.HashMap;
import java.util.Map;

public class UserInformation extends HashMap<String, Object> {

    public UserInformation(Map<? extends String, ?> map) {
        super(map);
    }

    public String getUserId(){
        return (String) get("sub");
    }
}
