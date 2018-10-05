package org.humanbrainproject.knowledgegraph.control.inference;

import org.humanbrainproject.knowledgegraph.control.arango.DatabaseController;
import org.humanbrainproject.knowledgegraph.entity.indexing.EditorInstance;
import org.humanbrainproject.knowledgegraph.entity.indexing.QualifiedGraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.indexing.ReconciledInstance;
import org.humanbrainproject.knowledgegraph.entity.indexing.TodoList;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class Reconciliation implements InferenceStrategy, InitializingBean{

    @Autowired
    InferenceController controller;

    @Autowired
    DatabaseController databaseController;

    @Autowired



    @Override
    public void afterPropertiesSet() throws Exception {
        controller.addInferenceStrategy(this);
    }

    @Override
    public boolean isInferenceNeeded(QualifiedGraphIndexingSpec spec) {
        if(spec.isEditorInstance()){
            List<String> parents = spec.asEditorInstance().getParents();
            return parents !=null && !parents.isEmpty();
        } else if(spec.isOriginalInstance()){
            //TODO check in arango interference if instance exists and if it contains editor link

        }
        return false;
    }

    @Override
    public TodoList infer(QualifiedGraphIndexingSpec spec) {
        ReconciledInstance instance;
        if(spec.isReconciledInstance()){
            instance = translateToReconciledInstance(spec);
        }
        else if(spec.isEditorInstance()){
            instance = calculateReconciledInstance(spec.asEditorInstance());
        }
        else {
            instance = calculateReconciledInstanceFromOriginalInstance(spec);
        }
        resolveInstances(instance);
        collectVirtualLinks(instance);
        reconcile(instance);
        return null;

    }


    //Collect all incoming links to all instances that combine to the reconciledInstance
    private void collectVirtualLinks(ReconciledInstance instance){
        instance.setVirtualLinks(Collections.emptySet());
    }

    private ReconciledInstance translateToReconciledInstance(QualifiedGraphIndexingSpec spec){
        return new ReconciledInstance(Collections.singleton(spec.getUrl()));
    }

    private ReconciledInstance calculateReconciledInstance(EditorInstance editor){
        Set<String> ids = new HashSet<>();
        List<String> parents = editor.getParents();
        if(parents!=null) {
            ids.addAll(editor.getParents());
        }
        ids.add(editor.getUrl());
        return new ReconciledInstance(ids);
    }

    private ReconciledInstance calculateReconciledInstanceFromOriginalInstance(QualifiedGraphIndexingSpec spec){
        Set<String> ids = new HashSet<>();
        List<String> editorInstances = getEditorInstancesForOriginalInstance(spec.getUrl());
        if(editorInstances!=null){
            ids.addAll(editorInstances);
        }
        ids.add(spec.getUrl());
        return new ReconciledInstance(ids);
    }

    private List<String> getEditorInstancesForOriginalInstance(String url){
        return Collections.emptyList();
    }

    private void resolveInstances(ReconciledInstance instance){
        instance.setResolvedInstancesByInstanceId(Collections.emptyMap());
    }

    private void reconcile(ReconciledInstance instance){

    }

}
