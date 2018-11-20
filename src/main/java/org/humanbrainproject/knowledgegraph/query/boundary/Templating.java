package org.humanbrainproject.knowledgegraph.query.boundary;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.query.control.FreemarkerTemplating;
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

    public void saveLibrary(String library, String libraryId, String template){
        templating.saveLibrary(library, libraryId, template, databaseFactory.getInternalDB());
    }

    public Template getTemplateById(StoredTemplateReference storedTemplateReference) {
        return templating.getTemplateById(storedTemplateReference, databaseFactory.getInternalDB());
    }


}
