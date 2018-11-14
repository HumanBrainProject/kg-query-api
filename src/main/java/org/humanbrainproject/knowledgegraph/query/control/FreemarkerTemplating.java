package org.humanbrainproject.knowledgegraph.query.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.gson.Gson;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.LibraryCollection;
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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Component
public class FreemarkerTemplating {

    @Autowired
    ArangoRepository repository;

    protected Logger logger = LoggerFactory.getLogger(FreemarkerTemplating.class);

    private final static ArangoCollectionReference TEMPLATES = new ArangoCollectionReference("templates");

    private Gson gson = new Gson();

    private String getTemplateContent(String name) throws URISyntaxException, IOException {
        return new String(Files.readAllBytes(Paths.get(getClass().getResource(String.format("/freemarker/%s.ftl", name)).toURI())));
    }

    public void saveTemplate(org.humanbrainproject.knowledgegraph.query.entity.Template template, ArangoConnection driver){
        saveFreemarker(gson.toJson(template.asJsonDocument()), template.getKey(), TEMPLATES, driver);
    }

    public void saveLibrary(String library, String libraryId, LibraryCollection collection, ArangoConnection driver){
        Map<String, String> lib = new HashMap<>();
        lib.put(collection.getCollectionName(), library);
        lib.put(ArangoVocabulary.KEY, libraryId);
        saveFreemarker(gson.toJson(lib), libraryId, collection.asArangoCollectionReference(), driver);
    }

    public String getLibraryById(String libraryId, LibraryCollection libraryCollection, ArangoConnection connection){
        ArangoDatabase db = connection.getOrCreateDB();
        ArangoCollection library = db.collection(libraryCollection.getCollectionName());
        if(library.exists() && library.documentExists(libraryId)){
            return (String)library.getDocument(libraryId, Map.class).get(libraryCollection.getCollectionName());
        }
        return null;
    }


    private void saveFreemarker(String document, String id, ArangoCollectionReference collectionReference, ArangoConnection driver){
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
        ArangoDatabase db = driver.getOrCreateDB();
        return db.collection(TEMPLATES.getName()).getDocument(ArangoNamingHelper.createCompatibleId(templateId.getName()),  org.humanbrainproject.knowledgegraph.query.entity.Template.class);
    }


    public String applyTemplate(String template, QueryResult<List<Map>> queryResult, StoredLibraryReference library, ArangoConnection driver) {
        String libraryById = library!=null && library.getName()!=null ? getLibraryById(library.getName(), library.getLibraryCollection(), driver) : null;
        return applyTemplate(template, queryResult, libraryById, repository.getAll(library.getLibraryCollection().asArangoCollectionReference(), org.humanbrainproject.knowledgegraph.query.entity.Template.class, driver));
    }

    String applyTemplate(String template, QueryResult<List<Map>> queryResult, String libraryContent, List<org.humanbrainproject.knowledgegraph.query.entity.Template> libraries) {
        //queryResult.getResults().forEach(this::replaceSpecialCharacters);
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
