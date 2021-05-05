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

package org.humanbrainproject.knowledgegraph.converters.entity;

import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.LinkedHashMap;
import java.util.Map;

public class EditorSpecField {

    private final String key;
    private String type;
    private final String label;
    private final String shapeDeclaration;
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    public EditorSpecField(String key, String label, String shapeDeclaration) {
        this.key = key;
        this.label = label;
        this.shapeDeclaration = shapeDeclaration;
    }

    public JsonDocument toJson() {
        JsonDocument doc = new JsonDocument();
        doc.addToProperty("key", key);
        doc.addToProperty("label", label);
        doc.addToProperty("_shapeDeclaration", shapeDeclaration);
        for (String s : additionalProperties.keySet()) {
            doc.addToProperty(s, additionalProperties.get(s));
        }
        if(doc.get("type")==null){
            doc.addToProperty("type", "InputText");
        }
        return doc;
    }


    public EditorSpecField setLinkToOtherInstance(boolean allowCustomValues){
        additionalProperties.put("type", "DropdownSelect");
        additionalProperties.put("closeDropdownAfterInteraction", true);
        additionalProperties.put("isLink", true);
        additionalProperties.put("mappingValue", "id");
        additionalProperties.put("mappingLabel", "name");
        additionalProperties.put("allowCustomValues", allowCustomValues);
        return this;
    }





}
