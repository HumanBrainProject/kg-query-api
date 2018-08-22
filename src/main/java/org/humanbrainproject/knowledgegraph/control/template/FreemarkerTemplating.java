package org.humanbrainproject.knowledgegraph.control.template;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.gson.Gson;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Component
@Scope(scopeName = "singleton")
public class FreemarkerTemplating {

    @Autowired
    ArangoRepository repository;

    protected Logger logger = LoggerFactory.getLogger(ArangoNamingConvention.class);

    private final static String TEMPLATES = "templates";
    private final static String LIBRARIES = "libraries";

    private Gson gson = new Gson();

    private String getTemplateContent(String name) throws URISyntaxException, IOException {
        return new String(Files.readAllBytes(Paths.get(getClass().getResource(String.format("/freemarker/%s.ftl", name)).toURI())));
    }

    public void saveTemplate(org.humanbrainproject.knowledgegraph.entity.Template template, ArangoDriver driver){
        saveFreemarker(template, TEMPLATES, driver);
    }

    public void saveLibrary(org.humanbrainproject.knowledgegraph.entity.Template template, ArangoDriver driver){
        saveFreemarker(template, LIBRARIES, driver);
    }

    private void saveFreemarker(org.humanbrainproject.knowledgegraph.entity.Template template, String collectionName, ArangoDriver driver){
        ArangoDatabase db = driver.getOrCreateDB();
        ArangoCollection collection = db.collection(collectionName);
        if(!collection.exists()){
            db.createCollection(collectionName);
        }
        if(collection.documentExists(template.get_key())){
            collection.replaceDocument(template.get_key(), gson.toJson(template));
        }
        else{
            String t = gson.toJson(template);
            collection.insertDocument(t);
        }
    }

    public org.humanbrainproject.knowledgegraph.entity.Template getTemplateById(String templateId, ArangoDriver driver){
        ArangoDatabase db = driver.getOrCreateDB();
        org.humanbrainproject.knowledgegraph.entity.Template document = db.collection(TEMPLATES).getDocument(templateId, org.humanbrainproject.knowledgegraph.entity.Template.class);
        return document;
    }


    public String applyTemplate(String template, QueryResult<List<Map>> queryResult, ArangoDriver driver) {
        return applyTemplate(template, queryResult, repository.getAll(LIBRARIES, org.humanbrainproject.knowledgegraph.entity.Template.class, driver));
    }

    String applyTemplate(String template, QueryResult<List<Map>> queryResult, List<org.humanbrainproject.knowledgegraph.entity.Template> libraries) {
        queryResult.getResults().forEach(this::replaceSpecialCharacters);
        try (StringWriter writer = new StringWriter(); StringReader reader = new StringReader(template)) {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
            cfg.setEncoding(Locale.ENGLISH, "utf-8");
            cfg.setURLEscapingCharset("utf-8");
            StringTemplateLoader stringLoader = new StringTemplateLoader();
            for (org.humanbrainproject.knowledgegraph.entity.Template library : libraries) {
                stringLoader.putTemplate(library.get_key(), library.getTemplateContent());
            }
            cfg.setTemplateLoader(stringLoader);
            Template t = new Template("dynamic", reader, cfg);
            t.process(queryResult, writer);
            return writer.toString();
        } catch (TemplateException | IOException e) {
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
