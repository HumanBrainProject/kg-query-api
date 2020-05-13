/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.ParameterDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoTests(NoTests.TRIVIAL)
public class Specification {
    private final String originalContext;
    private final String name;
    private final String rootSchema;
    private final List<SpecField> fields;
    private final JsonDocument originalDocument;
    private final FieldFilter documentFilter;


    public String getName() {
        return name;
    }

    public String getRootSchema() {
        return rootSchema;
    }

    public List<SpecField> getFields() {
        return fields;
    }

    public JsonDocument getOriginalDocument() {
        return originalDocument;
    }

    private String specificationId;

    public String getSpecificationId() {
        return specificationId;
    }

    public void setSpecificationId(String specificationId) {
        this.specificationId = specificationId;
    }

    public Specification(String originalContext, String name, String rootSchema, JsonDocument originalDocument, List<SpecField> fields, FieldFilter documentFilter) {
        this.originalContext = originalContext;
        this.name = name;
        this.rootSchema = rootSchema;
        this.originalDocument = originalDocument;
        this.fields = fields==null ? Collections.emptyList() : Collections.unmodifiableList(fields);
        this.documentFilter = documentFilter;
    }

    public FieldFilter getDocumentFilter() {
        return documentFilter;
    }

    public List<ParameterDescription> getAllFilterParameters(){
        List<ParameterDescription> filterParameters = findFilterParameters(this.getFields(), new ArrayList<>(), new ArrayList<>());
        if(this.documentFilter!=null && this.documentFilter.getParameter()!=null){
            filterParameters.add(new ParameterDescription(documentFilter.getParameter(), documentFilter.getOp(), Collections.emptyList()));
        }
        return filterParameters;
    }

    private static List<ParameterDescription> findFilterParameters(List<SpecField> fields, List<ParameterDescription> parameters, List<String> path){
        for (SpecField field : fields) {
            List<String> newPath = new ArrayList<>(path);
            newPath.add(field.fieldName);
            if(field.fieldFilter!=null && field.fieldFilter.getParameter()!=null){
                parameters.add(new ParameterDescription(field.fieldFilter.getParameter(), field.fieldFilter.getOp(), newPath));
            }
            if(field.fields!=null){
                findFilterParameters(field.fields, parameters, newPath);
            }
        }
        return parameters;
    }






}
