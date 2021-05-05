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

package org.humanbrainproject.knowledgegraph.query.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.gson.Gson;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.query.entity.StoredLibraryReference;
import org.humanbrainproject.knowledgegraph.query.entity.StoredTemplateReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@Component
@ToBeTested
public class FreemarkerTemplating {

    @Autowired
    ArangoRepository repository;

    @Autowired
    AuthorizationContext authorizationContext;


    protected Logger logger = LoggerFactory.getLogger(FreemarkerTemplating.class);

    private final static ArangoCollectionReference TEMPLATES = new ArangoCollectionReference("templates");

    private Gson gson = new Gson();

    public void saveTemplate(org.humanbrainproject.knowledgegraph.query.entity.Template template, ArangoConnection driver){
        saveFreemarker(gson.toJson(template.asJsonDocument()), template.getKey(), TEMPLATES, driver);
    }

    public void saveLibrary(String library, String libraryId, String template, ArangoConnection driver){
        Map<String, String> lib = new HashMap<>();
        lib.put(ArangoVocabulary.LIBRARY, library);
        lib.put(ArangoVocabulary.KEY, template);
        saveFreemarker(gson.toJson(lib), template, new ArangoCollectionReference(ArangoNamingHelper.createCompatibleId("libraries-"+libraryId)), driver);
    }

    public String getLibraryById(String libraryId, String template, ArangoConnection connection){
        //TODO ensure authorization

        ArangoDatabase db = connection.getOrCreateDB();
        ArangoCollection library = db.collection(ArangoNamingHelper.createCompatibleId("libraries-"+libraryId));
        if(library.exists() && library.documentExists(template)){
            return (String)library.getDocument(template, Map.class).get(ArangoVocabulary.LIBRARY);
        }
        return null;
    }


    private void saveFreemarker(String document, String id, ArangoCollectionReference collectionReference, ArangoConnection driver){
        //TODO ensure authorization
        ArangoDatabase db = driver.getOrCreateDB();
        ArangoCollection collection = db.collection(collectionReference.getName());
        if(!collection.exists()){
            db.createCollection(collectionReference.getName());
        }
        String key = ArangoNamingHelper.createCompatibleId(id);
        if(collection.documentExists(key)){
            collection.replaceDocument(key, document);
        }
        else{
            collection.insertDocument(document);
        }
    }

    public  org.humanbrainproject.knowledgegraph.query.entity.Template getTemplateById(StoredTemplateReference templateId, ArangoConnection driver){
        //TODO ensure authorization

        ArangoDatabase db = driver.getOrCreateDB();
        return db.collection(TEMPLATES.getName()).getDocument(ArangoNamingHelper.createCompatibleId(templateId.getName()),  org.humanbrainproject.knowledgegraph.query.entity.Template.class);
    }


    public String applyTemplate(String template, QueryResult<List<Map>> queryResult, StoredLibraryReference library, ArangoConnection driver) {
        String libraryById = library!=null && library.getName()!=null ? getLibraryById(library.getName(), library.getTemplate(), driver) : null;
        return applyTemplate(template, queryResult, libraryById);
    }

    String applyTemplate(String template, QueryResult<List<Map>> queryResult, String libraryContent) {
        String finalTemplate;
        if(libraryContent!=null){
            finalTemplate = String.format("%s\n\n%s", libraryContent, template);
        }
        else{
            finalTemplate = template;
        }

        try (StringWriter writer = new StringWriter(); StringReader reader = new StringReader(finalTemplate)) {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
            cfg.setEncoding(Locale.ENGLISH, "utf-8");
            cfg.setURLEscapingCharset("utf-8");
            Template t = new Template("dynamic", reader, cfg);
            t.process(queryResult, writer);
            return writer.toString();
        } catch (TemplateException | IOException e) {
            logger.error("Was not able to apply template {}", finalTemplate);
            throw new RuntimeException("Was not able to apply template", e);
        }
    }

    private String replaceSpecialChars(String original){
        return original.replaceAll(":", "_");
    }

    private void replaceSpecialCharacters(Object o){
        if(o instanceof Map){
            Map map = (Map)o;
            Set<Object> keys = new HashSet<Object>();
            keys.addAll(map.keySet());
            for (Object key : keys) {
                replaceSpecialCharacters(map.get(key));
                if(key instanceof String){
                    String newkey = replaceSpecialChars((String)key);
                    if(!key.equals(newkey)){
                        map.put(newkey, map.get(key));
                        map.remove(key);
                    }
                }
            }
        }
        else if(o instanceof Collection) {
            Collection c = (Collection) o;
            for (Object el : c) {
                replaceSpecialCharacters(el);
            }
        }
    }

}
