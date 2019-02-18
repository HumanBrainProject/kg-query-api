package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Tested
public class SpecField {

    public String fieldName;
    public final List<SpecField> fields;
    public final List<SpecTraverse> traversePath;
    public boolean required;
    public boolean sortAlphabetically;
    public boolean groupby;
    public boolean ensureOrder;
    public final String groupedInstances;
    public FieldFilter fieldFilter;


    public SpecField(String fieldName, List<SpecField> fields, List<SpecTraverse> traversePath, String groupedInstances, boolean required, boolean sortAlphabetically, boolean groupby, boolean ensureOrder, FieldFilter fieldFilter) {
        this.fieldName = fieldName;
        this.required = required;
        this.fields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
        this.traversePath = traversePath==null ? Collections.emptyList() : Collections.unmodifiableList(traversePath);
        this.sortAlphabetically = sortAlphabetically;
        this.groupby = groupby;
        this.groupedInstances = groupedInstances;
        this.ensureOrder = ensureOrder;
        this.fieldFilter = fieldFilter;
    }

    public boolean isDirectChild(){
        return !hasSubFields() && traversePath.size()<2;
    }


    public boolean hasSubFields(){
        //TODO check how to handle merges
        return fields!=null && !fields.isEmpty();
    }


    public String getGroupedInstances() {
        return groupedInstances;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isGroupby() {
        return groupby;
    }

    public boolean isLeaf(){
        return fields.isEmpty();
    }

    public boolean isMerge(){
        return this.traversePath.isEmpty() && !this.fields.isEmpty();
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


    public boolean hasGrouping(){
        if(groupedInstances!=null && !groupedInstances.isEmpty() && fields!=null && !fields.isEmpty()){
            for (SpecField field : fields) {
                if(field.isGroupby()){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasNestedGrouping(){
        if(fields!=null && !fields.isEmpty()){
            for (SpecField field : fields) {
                if(field.isGroupby() || field.hasNestedGrouping()){
                    return true;
                }
            }
        }
        return false;
    }
}
