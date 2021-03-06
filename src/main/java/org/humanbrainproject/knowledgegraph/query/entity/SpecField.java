/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public final Map<String, Object> customDirectives;


    public SpecField(String fieldName, List<SpecField> fields, List<SpecTraverse> traversePath, String groupedInstances, boolean required, boolean sortAlphabetically, boolean groupby, boolean ensureOrder, FieldFilter fieldFilter) {
        this(fieldName, fields, traversePath, groupedInstances, required, sortAlphabetically, groupby, ensureOrder, fieldFilter, null);
    }

    public SpecField(String fieldName, List<SpecField> fields, List<SpecTraverse> traversePath, String groupedInstances, boolean required, boolean sortAlphabetically, boolean groupby, boolean ensureOrder, FieldFilter fieldFilter, Map<String, Object> customDirectives) {
        this.fieldName = fieldName;
        this.required = required;
        this.fields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
        this.traversePath = traversePath==null ? Collections.emptyList() : Collections.unmodifiableList(traversePath);
        this.sortAlphabetically = sortAlphabetically;
        this.groupby = groupby;
        this.groupedInstances = groupedInstances;
        this.ensureOrder = ensureOrder;
        this.fieldFilter = fieldFilter;
        this.customDirectives = customDirectives;
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

    public List<SpecField> getSubFieldsWithSort(){
        if(fields!=null && !fields.isEmpty()){
            return fields.stream().filter(SpecField::isSortAlphabetically).collect(Collectors.toList());
        }
        return Collections.emptyList();

    }

}
