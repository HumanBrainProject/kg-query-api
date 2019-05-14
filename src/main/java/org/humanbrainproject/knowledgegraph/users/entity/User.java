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