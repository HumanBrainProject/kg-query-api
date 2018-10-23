package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import org.humanbrainproject.knowledgegraph.indexing.control.IndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.entity.InstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.TargetDatabase;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.MainVertex;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Reconciliation implements InferenceStrategy, InitializingBean {


    public final static String ORIGINAL_PARENT_PROPERTY = "http://hbp.eu/reconciled#original_parent";

    @Autowired
    InferenceController controller;

    @Autowired
    ArangoDatabaseFactory databaseController;

    @Autowired
    ArangoRepository repository;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    MessageProcessor graphSpecificationController;


    @Autowired
    IndexingProvider<?> indexingProvider;


    @Override
    public void afterPropertiesSet() {
        controller.addInferenceStrategy(this);
    }

    @Override
    public void infer(QualifiedIndexingMessage message, Set<MainVertex> documents) {

        //We collect all instances from the default space
        InstanceReference originalId = indexingProvider.findOriginalId(message.getOriginalMessage().getInstanceReference());
        MainVertex originalStructure = indexingProvider.getVertexStructureById(originalId, TargetDatabase.DEFAULT);
        Set<InstanceReference> relativeInstances = indexingProvider.findInstancesWithLinkTo(ORIGINAL_PARENT_PROPERTY, originalId);
        Set<MainVertex> relativeInstanceStructures = relativeInstances.stream().map(relativeInstance -> indexingProvider.getVertexStructureById(relativeInstance, TargetDatabase.DEFAULT)).collect(Collectors.toSet());

        //Now we apply the inference logic

        //And we add the inferred instance to the collection of documents

    }

}
