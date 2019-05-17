package org.humanbrainproject.knowledgegraph.scopes.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.entity.JsonLdObject;
import org.humanbrainproject.knowledgegraph.commons.entity.JsonLdStructure;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.AbsoluteNexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.Map;

public class Invitation extends JsonLdObject {

    public static final JsonLdStructure<Invitation> STRUCTURE = new JsonLdStructure(new NexusSchemaReference("hbpkg", "core", "invitation", "v1.0.0"), Invitation.class);
    public static final String USER_FIELDNAME = STRUCTURE.getFieldNameInNamespace("user");
    public static final String INSTANCE_FIELDNAME = STRUCTURE.getFieldNameInNamespace("instance");

    private final AbsoluteNexusInstanceReference user;
    private final AbsoluteNexusInstanceReference instance;


    public Invitation(AbsoluteNexusInstanceReference user, AbsoluteNexusInstanceReference instance) {
        super(STRUCTURE);
        this.user = user;
        this.instance = instance;
    }

    private AbsoluteNexusInstanceReference getSingleReferenceFromPayload(JsonDocument jsonDocument, String field){
        Object object = jsonDocument.get(field);
        if(object instanceof Map){
            Object id = ((Map) object).get(JsonLdConsts.ID);
            if(id instanceof String){
                return new AbsoluteNexusInstanceReference(NexusInstanceReference.createFromUrl((String)id), (String)id);
            }
        }
        return null;
    }

    public Invitation(JsonLdStructure jsonLdStructure, JsonDocument fromDB) {
        super(jsonLdStructure, fromDB);
        this.user = getSingleReferenceFromPayload(fromDB, USER_FIELDNAME);
        this.instance = getSingleReferenceFromPayload(fromDB, INSTANCE_FIELDNAME);
    }

    @Override
    protected void addFieldsToJson(JsonDocument jsonDocument) {
        jsonDocument.addReference(USER_FIELDNAME, user.getAbsoluteUrl());
        jsonDocument.addReference(INSTANCE_FIELDNAME, instance.getAbsoluteUrl());
    }

    public AbsoluteNexusInstanceReference getUser() {
        return user;
    }

    public AbsoluteNexusInstanceReference getInstance() {
        return instance;
    }
}
