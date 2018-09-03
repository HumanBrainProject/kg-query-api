package org.humanbrainproject.knowledgegraph.boundary.indexing;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.arango.DatabaseController;
import org.humanbrainproject.knowledgegraph.control.indexing.GraphSpecificationController;
import org.humanbrainproject.knowledgegraph.control.releasing.ReleasingController;
import org.humanbrainproject.knowledgegraph.control.spatialSearch.SpatialSearchController;
import org.humanbrainproject.knowledgegraph.entity.indexing.GraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.indexing.QualifiedGraphIndexingSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class GraphIndexing {

    @Autowired
    ArangoRepository repository;

    @Autowired
    ReleasingController releasingController;

    @Autowired
    SpatialSearchController spatialSearchController;

    @Autowired
    GraphSpecificationController graphSpecificationController;

    @Autowired
    DatabaseController databaseController;


    private Logger logger = LoggerFactory.getLogger(GraphIndexing.class);

    public void insertJsonOrJsonLd(GraphIndexingSpec spec) {
        QualifiedGraphIndexingSpec qualifiedSpec = graphSpecificationController.qualify(spec);
        repository.uploadToPropertyGraph(qualifiedSpec, databaseController.getDefaultDB());
        spatialSearchController.index(qualifiedSpec.asSpatialAnchoring());
        releasingController.releaseVertices(qualifiedSpec.asRelease());
    }

    public void updateJsonOrJsonLd(GraphIndexingSpec spec) {
        QualifiedGraphIndexingSpec qualifiedSpec = graphSpecificationController.qualify(spec);
        Set<String> verticesToBeUnreleased = qualifiedSpec.asRelease() != null ? releasingController.findDocumentIdsToBeUnreleased(qualifiedSpec.asRelease()) : null;
        repository.uploadToPropertyGraph(qualifiedSpec, databaseController.getDefaultDB());
        spatialSearchController.index(qualifiedSpec.asSpatialAnchoring());
        releasingController.releaseVertices(qualifiedSpec.asRelease());
        releasingController.unreleaseDocumentsById(verticesToBeUnreleased);
    }

    public void delete(GraphIndexingSpec spec) {
        Map instance = repository.getByKey(spec.getEntityName(), spec.getId(), Map.class, databaseController.getDefaultDB());
        if (instance != null) {
            QualifiedGraphIndexingSpec qualifiedSpec = graphSpecificationController.qualify(spec, instance);
            releasingController.unreleaseVertices(qualifiedSpec.asRelease());
            releasingController.unreleaseInstance(qualifiedSpec.getUrl(), false);
            spatialSearchController.remove(qualifiedSpec.asSpatialAnchoring());
            repository.deleteVertex(spec.getEntityName(), spec.getId(), databaseController.getDefaultDB());
        } else {
            logger.error("DEL: Was not able to find entity {}/{} in repository", spec.getEntityName(), spec.getId());
        }
    }

    public String getById(String entityName, String id) {
        return repository.getByKey(entityName, id, String.class, databaseController.getDefaultDB());
    }

    public void clearGraph() {
       databaseController.clearGraph();
    }

}
