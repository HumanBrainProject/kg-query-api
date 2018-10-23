package org.humanbrainproject.knowledgegraph.query.boundary;

import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.query.control.FreemarkerTemplating;
import org.humanbrainproject.knowledgegraph.query.entity.StoredQueryReference;
import org.humanbrainproject.knowledgegraph.query.entity.StoredTemplateReference;
import org.humanbrainproject.knowledgegraph.query.entity.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Templating {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    FreemarkerTemplating templating;

    public void saveTemplate(Template template){
        templating.saveTemplate(template, databaseFactory.getInternalDB());
    }

    public void saveLibrary(Template template){
        templating.saveLibrary(template, databaseFactory.getInternalDB());
    }

    public Template getTemplateById(StoredQueryReference storedQueryReference, StoredTemplateReference storedTemplateReference) {
        return templating.getTemplateById(getTemplateQueryId(storedQueryReference, storedTemplateReference), databaseFactory.getInternalDB());
    }

    private String getTemplateQueryId(StoredQueryReference queryReference, StoredTemplateReference templateReference){
        return String.format("%s_%s", queryReference.getName(), templateReference.getName());
    }

}
