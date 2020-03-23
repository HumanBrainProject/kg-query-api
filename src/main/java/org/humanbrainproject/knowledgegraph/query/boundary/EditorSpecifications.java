package org.humanbrainproject.knowledgegraph.query.boundary;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.query.control.EditorSpecificationsController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class EditorSpecifications {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    EditorSpecificationsController editorSpecificationsController;

    public void saveSpecification(String specification, String specificationId) {
        editorSpecificationsController.saveSpecification(specification, specificationId, databaseFactory.getInternalDB());
    }

}
