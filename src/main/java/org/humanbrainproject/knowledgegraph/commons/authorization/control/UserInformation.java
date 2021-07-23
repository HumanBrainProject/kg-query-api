/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

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

    public String getUserName(){
        return (String) get("preferred_username");
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

    public boolean hasReleasedPermission(){
        Object groups = get("groups");
        List<String> g = null;
        if(groups instanceof List){
            g = (List)groups;
        }
        else if(groups instanceof String){
            g = Arrays.asList(((String) groups).split(","));
        }
        return g!=null && g.contains("kg-releasedInstances");
    }

}
