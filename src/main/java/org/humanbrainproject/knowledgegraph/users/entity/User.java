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

package org.humanbrainproject.knowledgegraph.users.entity;

import org.humanbrainproject.knowledgegraph.commons.entity.JsonLdObject;
import org.humanbrainproject.knowledgegraph.commons.entity.JsonLdStructure;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.Map;

public class User extends JsonLdObject {

    public final static JsonLdStructure<User> STRUCTURE = new JsonLdStructure(new NexusSchemaReference("hbpkg", "core", "user", "v0.0.1"), User.class);
    public final static String USER_ID_FIELD = STRUCTURE.getFieldNameInNamespace("userId");

    private final String userId;

    public User(String userId) {
        super(STRUCTURE);
        this.userId = userId;
    }

    public User(JsonLdStructure jsonLdStructure, JsonDocument fromDB) {
        super(jsonLdStructure, fromDB);
        this.userId = (String)fromDB.get(USER_ID_FIELD);
    }

    @Override
    protected void addFieldsToJson(JsonDocument jsonDocument) {
        jsonDocument.addToProperty(USER_ID_FIELD, userId);
    }

    public String getUserId() {
        return userId;
    }
}