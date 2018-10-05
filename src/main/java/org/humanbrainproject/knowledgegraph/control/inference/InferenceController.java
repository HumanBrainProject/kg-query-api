package org.humanbrainproject.knowledgegraph.control.inference;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.indexing.GraphSpecificationController;
import org.humanbrainproject.knowledgegraph.entity.indexing.QualifiedGraphIndexingSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InferenceController {

    @Autowired
    ArangoRepository repository;

    @Autowired
    @Qualifier("reconciled")
    ArangoDriver reconciled;

    @Autowired
    GraphSpecificationController graphSpecificationController;

    private Set<InferenceStrategy> strategies = Collections.synchronizedSet(new HashSet<>());

    void addInferenceStrategy(InferenceStrategy strategy){
        strategies.add(strategy);
    }

    public void infer(QualifiedGraphIndexingSpec spec){
        if(spec.isReconciledInstance()){
            createAndUploadInferedObjectToPropertyGraph(spec);
        }
        else {
            Set<InferenceStrategy> strategiesWithInference = strategies.parallelStream().filter(s -> s.isInferenceNeeded(spec)).collect(Collectors.toSet());
            if (strategiesWithInference.isEmpty()) {
                createAndUploadInferedObjectToPropertyGraph(spec);
            } else {
                applyAndPersistInferedEntityInNexus(spec, strategiesWithInference);
                //No need for an upload to the property graph since the insertion into nexus will trigger this later on.
            }
        }
    }

    private void applyAndPersistInferedEntityInNexus(QualifiedGraphIndexingSpec spec, Set<InferenceStrategy> strategies){

    }


    private void createAndUploadInferedObjectToPropertyGraph(QualifiedGraphIndexingSpec spec){
        QualifiedGraphIndexingSpec translatedToMainSpace = graphSpecificationController.qualify(spec.getSpec(), true);
        repository.uploadToPropertyGraph(translatedToMainSpace, reconciled);
    }


}
