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
