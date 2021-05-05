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

package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.indexing.boundary.GraphIndexing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Tested
public class JsonTransformer {

    Gson gson = new Gson();
    private Logger logger = LoggerFactory.getLogger(JsonTransformer.class);

    public String getMapAsJson(Map map){
        return gson.toJson(map);
    }

    /**
     * @return a single map if available. If the json string describes a (non-empty) list, the first item is returned. If the json is not a dict, null is returned
     */
    public Map parseToMap(String json) {
        try {
            Object o = gson.fromJson(json, Object.class);
            if (o instanceof Map) {
                return (Map) o;
            } else if (o instanceof List && !((List) o).isEmpty()) {
                Object firstElement = ((List) o).get(0);
                if (firstElement instanceof Map) {
                    return (Map) firstElement;
                }
            }
            return null;
        }
        catch(RuntimeException e){
            logger.error(String.format("Was not able to parse JSON:\n%s", json), e);
            throw e;
        }
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
            return null;
        }
    }

    public String normalize(String json){
        return gson.toJson(gson.fromJson(json, Object.class));
    }

}
