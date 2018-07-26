package org.humanbrainproject.knowledgegraph.control.specification;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.entity.specification.SpecField;
import org.humanbrainproject.knowledgegraph.entity.specification.SpecTraverse;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SpecificationInterpreter {

   private static final String GRAPH_QUERY_VOCAB = "http://schema.hbp.eu/graph_query";
   private static final String GRAPH_QUERY_ROOT_SCHEMA = GRAPH_QUERY_VOCAB+"/root_schema";
   private static final String GRAPH_QUERY_FIELDNAME = GRAPH_QUERY_VOCAB+"/fieldname";
   private static final String GRAPH_QUERY_RELATIVE_PATH = GRAPH_QUERY_VOCAB+"/relative_path";
   private static final String GRAPH_QUERY_FIELDS = GRAPH_QUERY_VOCAB+"/fields";
   private static final String GRAPH_QUERY_REVERSE = GRAPH_QUERY_VOCAB+"/reverse";
   private static final String SCHEMA_ORG_NAME = "http://schema.org/name";

   public Specification readSpecification(JSONObject jsonObject) throws JSONException {
       String originalContext = null;
       if(jsonObject.has(JsonLdConsts.CONTEXT)){
           originalContext = jsonObject.getString(JsonLdConsts.CONTEXT);
       }
       String name = null;
       if(jsonObject.has(SCHEMA_ORG_NAME)){
           name = jsonObject.getString(SCHEMA_ORG_NAME);
       }
       String rootSchema = null;
       if(jsonObject.has(GRAPH_QUERY_ROOT_SCHEMA)){
           rootSchema = jsonObject.getJSONObject(GRAPH_QUERY_ROOT_SCHEMA).getString(JsonLdConsts.ID);
       }
       List<SpecField> specFields = null;
       if(jsonObject.has(GRAPH_QUERY_FIELDS)) {
           specFields = createSpecFields(jsonObject.get(GRAPH_QUERY_FIELDS));
           System.out.println(specFields);
       }
       return new Specification(originalContext,  name, rootSchema, specFields);
   }

   private List<SpecField> createSpecFields(Object origin) throws JSONException {
       List<SpecField> result = new ArrayList<>();
       if(origin instanceof JSONArray){
           JSONArray originArray = (JSONArray)origin;
           for(int i=0; i<originArray.length(); i++){
               result.addAll(createSpecFields(originArray.get(i)));
           }
       }
       else if(origin instanceof JSONObject){
           JSONObject originObj = (JSONObject)origin;
           if(originObj.has(GRAPH_QUERY_RELATIVE_PATH)) {
               Object relativePath = originObj.get(GRAPH_QUERY_RELATIVE_PATH);
               if (relativePath != null) {
                   List<SpecTraverse> traversalPath = createTraversalPath(relativePath);
                   if (traversalPath != null && !traversalPath.isEmpty()) {

                       String fieldName = null;
                       List<SpecField> specFields = null;
                       if(originObj.has(GRAPH_QUERY_FIELDNAME)) {
                           fieldName = originObj.getJSONObject(GRAPH_QUERY_FIELDNAME).getString(JsonLdConsts.ID);
                       }
                       if (fieldName == null) {
                           //Fall back to the name of the last traversal item if the fieldname is not defined.
                           fieldName = traversalPath.get(traversalPath.size() - 1).pathName;
                       }
                       if(originObj.has(GRAPH_QUERY_FIELDS)) {
                           specFields = createSpecFields(originObj.get(GRAPH_QUERY_FIELDS));
                       }
                       return Collections.singletonList(new SpecField(fieldName, specFields, traversalPath));
                   }
               }
           }
       }
       return result;
   }

   private Object removeAtId(Object object) throws JSONException {
       if(object instanceof JSONObject && ((JSONObject)object).has(JsonLdConsts.ID)){
           return ((JSONObject)object).get(JsonLdConsts.ID);
       }
       return object;

   }

   private List<SpecTraverse> createTraversalPath(Object relativePath) throws JSONException {
       List<SpecTraverse> result = new ArrayList<>();
       if(relativePath instanceof JSONArray){
           JSONArray relativePathArray = (JSONArray) relativePath;
           for(int i=0; i<relativePathArray.length(); i++){
                Object relativePathElement = relativePathArray.get(i);
                if(relativePathElement!=null) {
                    result.add(createSpecTraverse(relativePathElement));
                }
           }
       }
       else{
           result.add(createSpecTraverse(relativePath));
       }
       return result;
   }

    private SpecTraverse createSpecTraverse(Object relativePathElement) throws JSONException {
        String path = null;
        boolean reverse = false;

        if (relativePathElement instanceof JSONObject && ((JSONObject) relativePathElement).has(JsonLdConsts.ID)) {
            path = ((JSONObject) relativePathElement).getString(JsonLdConsts.ID);
            if (((JSONObject) relativePathElement).has(GRAPH_QUERY_REVERSE)) {
                reverse = ((JSONObject) relativePathElement).getBoolean(GRAPH_QUERY_REVERSE);
            }
        } else {
            path = relativePathElement.toString();
        }
        return new SpecTraverse(path, reverse);
    }

}
