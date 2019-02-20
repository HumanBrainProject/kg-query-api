package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.ParameterDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoTests(NoTests.TRIVIAL)
public class Specification {
    public final String originalContext;
    public final String name;
    public final String rootSchema;
    public final List<SpecField> fields;
    public final JsonDocument originalDocument;
    public final FieldFilter documentFilter;

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
        List<ParameterDescription> filterParameters = findFilterParameters(this.fields, new ArrayList<>(), new ArrayList<>());
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
