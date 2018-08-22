package org.humanbrainproject.knowledgegraph.boundary.query;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoQueryBuilder;
import org.humanbrainproject.knowledgegraph.control.arango.query.ArangoSpecificationQuery;
import org.humanbrainproject.knowledgegraph.control.authorization.AuthorizationController;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.control.template.FreemarkerTemplating;
import org.humanbrainproject.knowledgegraph.control.template.MustacheTemplating;
import org.humanbrainproject.knowledgegraph.entity.Template;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Templating {

    @Autowired
    @Qualifier("internal")
    ArangoDriver arango;

    @Autowired
    FreemarkerTemplating templating;

    Gson gson = new Gson();

    public void saveTemplate(Template template){
        templating.saveTemplate(template, arango);
    }

    public void saveLibrary(Template template){
        templating.saveLibrary(template, arango);
    }

    public Template getTemplateById(String templateId) {
        return templating.getTemplateById(templateId, arango);
    }

}
