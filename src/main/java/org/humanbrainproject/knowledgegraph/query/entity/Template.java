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
