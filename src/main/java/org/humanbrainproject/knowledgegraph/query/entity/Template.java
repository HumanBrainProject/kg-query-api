package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;

@Tested
public class Template {


    String key;
    String templateContent;
    String library;

    public Template() {
    }

    public Template(StoredQueryReference storedQueryReference, String templateId, String templateContent, String library) {
        this.key = new StoredTemplateReference(storedQueryReference, templateId).getName();
        this.templateContent = templateContent;
        this.library = library;
    }

    public String getLibrary() {
        return library;
    }

    public String getKey() {
        return key;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public JsonDocument asJsonDocument(){
        JsonDocument doc = new JsonDocument();
        doc.put(ArangoVocabulary.KEY, ArangoNamingHelper.createCompatibleId(this.key));
        doc.put("templateContent", this.templateContent);
        doc.put("library", this.library);
        return doc;
    }

}
