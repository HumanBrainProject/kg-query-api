package org.humanbrainproject.knowledgegraph.commons.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

public abstract class JsonLdObject {

    private final JsonLdStructure jsonLdStructure;
    private NexusInstanceReference instanceReference;


    public JsonLdObject(JsonLdStructure jsonLdStructure) {
        this.jsonLdStructure = jsonLdStructure;
    }

    public JsonLdObject(JsonLdStructure jsonLdStructure, JsonDocument fromDB){
        this(jsonLdStructure);
        this.instanceReference = NexusInstanceReference.createFromUrl((String)fromDB.get(JsonLdConsts.ID));
    }

    public final JsonDocument asJsonLd(){
        JsonDocument jsonDocument = new JsonDocument();
        jsonDocument.addType(jsonLdStructure.getType());
        addFieldsToJson(jsonDocument);
        return jsonDocument;
    }

    protected abstract void addFieldsToJson(JsonDocument jsonDocument);

    public JsonLdStructure getJsonLdStructure() {
        return jsonLdStructure;
    }

    public NexusInstanceReference getInstanceReference() {
        return instanceReference;
    }

    public void setInstanceReference(NexusInstanceReference instanceReference) {
        this.instanceReference = instanceReference;
    }
}
