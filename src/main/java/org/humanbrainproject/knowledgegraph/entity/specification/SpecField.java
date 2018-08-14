package org.humanbrainproject.knowledgegraph.entity.specification;

import java.util.Collections;
import java.util.List;

public class SpecField {

    public final String fieldName;
    public final List<SpecField> fields;
    public final List<SpecTraverse> traversePath;
    public final boolean required;
    public final boolean sortAlphabetically;

    public SpecField(String fieldName, List<SpecField> fields, List<SpecTraverse> traversePath, boolean required, boolean sortAlphabetically) {
        this.fieldName = fieldName;
        this.required = required;
        this.fields = fields == null ? Collections.emptyList() : Collections.unmodifiableList(fields);
        this.traversePath = Collections.unmodifiableList(traversePath);
        this.sortAlphabetically = sortAlphabetically;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isLeaf(){
        return fields.isEmpty();
    }

    public boolean needsTraversal(){
        return !fields.isEmpty() || this.traversePath.size()>1;
    }

    public SpecTraverse getFirstTraversal(){
        return !traversePath.isEmpty() ? traversePath.get(0) : null;
    }

    public List<SpecTraverse> getAdditionalDirectTraversals(){
        int numberOfDirectTraversals = numberOfDirectTraversals();
        if(numberOfDirectTraversals-1>0 && traversePath.size()>1){
            return traversePath.subList(1, numberOfDirectTraversals);
        }
        else{
            return Collections.emptyList();
        }
    }

    public SpecTraverse getLeafPath(){
        if(isLeaf() && !traversePath.isEmpty()){
            return traversePath.get(traversePath.size()-1);
        }
        return null;
    }

    public int numberOfDirectTraversals(){
        if(!needsTraversal()){
            return 0;
        }
        if(isLeaf()){
            return this.traversePath.size()-1;
        }
        else{
            return this.traversePath.size();
        }
    }

    public boolean isSortAlphabetically() {
        return sortAlphabetically;
    }
}
