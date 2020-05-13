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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.springframework.util.DigestUtils;

@ToBeTested(easy = true)
public class ArangoNamingHelper {


    public static final int MAX_CHARACTERS = 60;


    static String replaceSpecialCharacters(String value) {
        return value != null ? value.replaceAll("\\.", "_").replaceAll("[^a-zA-Z0-9\\-_]", "-") : null;
    }

    static String reduceStringToMaxSizeByHashing(String string) {
        return string == null || string.length() <= MAX_CHARACTERS ? string : String.format("hashed_%s", DigestUtils.md5DigestAsHex(string.getBytes()));
    }

    static String removeTrailingHttps(String value){
        return value!=null ? value.replaceAll("http(s)?://", "") : value;
    }

    public static String createCompatibleId(String id){
        return reduceStringToMaxSizeByHashing(removeTrailingHttps(replaceSpecialCharacters(id)));
    }
}
