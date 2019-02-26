package org.humanbrainproject.knowledgegraph.query.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.*;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Op;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Parameter;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

@ToBeTested
@Component
public class SpecificationInterpreter {


    protected Logger logger = LoggerFactory.getLogger(SpecificationInterpreter.class);

    public Specification readSpecification(String json, String absoluteUrlOfRootSchema, Map<String, String> allParameters) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        String originalContext = null;
        if (jsonObject.has(JsonLdConsts.CONTEXT)) {
            originalContext = jsonObject.getString(JsonLdConsts.CONTEXT);
        }
        String name = null;
        if (jsonObject.has(SchemaOrgVocabulary.NAME)) {
            name = jsonObject.getString(SchemaOrgVocabulary.NAME);
        }
        String rootSchema = null;
        if (absoluteUrlOfRootSchema != null) {
            rootSchema = absoluteUrlOfRootSchema;
        } else if (jsonObject.has(GraphQueryKeys.GRAPH_QUERY_ROOT_SCHEMA.getFieldName())) {
            rootSchema = jsonObject.getJSONObject(GraphQueryKeys.GRAPH_QUERY_ROOT_SCHEMA.getFieldName()).getString(JsonLdConsts.ID);
        }
        List<SpecField> specFields = null;
        if (jsonObject.has(GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName())) {
            specFields = createSpecFields(jsonObject.get(GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName()), allParameters);
        }
        FieldFilter fieldFilter = null;
        if (jsonObject.has(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName())) {
            fieldFilter = createFieldFilter(jsonObject.getJSONObject(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName()));
        }
        return new Specification(originalContext, name, rootSchema, new JsonDocument(new JsonTransformer().parseToMap(json)), specFields, fieldFilter);
    }


    private List<SpecField> createSpecFields(Object origin, Map<String, String> allParameters) throws JSONException {
        List<SpecField> result = new ArrayList<>();
        if (origin instanceof JSONArray) {
            JSONArray originArray = (JSONArray) origin;
            for (int i = 0; i < originArray.length(); i++) {
                result.addAll(createSpecFields(originArray.get(i), allParameters));
            }
        } else if (origin instanceof JSONObject) {
            JSONObject originObj = (JSONObject) origin;
            List<Object> allRelativePaths = null;
            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_MERGE.getFieldName())) {
                allRelativePaths = getAllRelativePaths(originObj.get(GraphQueryKeys.GRAPH_QUERY_MERGE.getFieldName()));
            } else if (originObj.has(GraphQueryKeys.GRAPH_QUERY_RELATIVE_PATH.getFieldName())) {
                allRelativePaths = Collections.singletonList(originObj.get(GraphQueryKeys.GRAPH_QUERY_RELATIVE_PATH.getFieldName()));
            }
            if (allRelativePaths != null && !allRelativePaths.isEmpty()) {
                List<SpecField> fieldsPerRelativePath = new ArrayList<>();
                for (Object relativePath : allRelativePaths) {
                    if (relativePath != null) {
                        List<SpecTraverse> traversalPath = createTraversalPath(relativePath);
                        if (traversalPath != null && !traversalPath.isEmpty()) {
                            String fieldName = null;
                            List<SpecField> specFields = null;
                            boolean required = false;
                            boolean sortAlphabetically = false;
                            boolean groupBy = false;
                            boolean ensureOrder = false;
                            FieldFilter fieldFilter = null;
                            String groupedInstances = GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES_DEFAULT.getFieldName();
                            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName())) {
                                fieldName = originObj.getJSONObject(GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName()).getString(JsonLdConsts.ID);
                            }
                            if (fieldName == null) {
                                //Fall back to the name of the last traversal item if the fieldname is not defined.
                                fieldName = traversalPath.get(traversalPath.size() - 1).pathName;
                            }
                            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName())) {
                                specFields = createSpecFields(originObj.get(GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName()), allParameters);
                            }
                            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_REQUIRED.getFieldName())) {
                                required = originObj.getBoolean(GraphQueryKeys.GRAPH_QUERY_REQUIRED.getFieldName());
                            }
                            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_SORT.getFieldName())) {
                                sortAlphabetically = originObj.getBoolean(GraphQueryKeys.GRAPH_QUERY_SORT.getFieldName());
                            }
                            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_ENSURE_ORDER.getFieldName())) {
                                ensureOrder = originObj.getBoolean(GraphQueryKeys.GRAPH_QUERY_ENSURE_ORDER.getFieldName());
                            }
                            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES.getFieldName())) {
                                groupedInstances = originObj.getJSONObject(GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES.getFieldName()).getString(JsonLdConsts.ID);
                            }
                            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_GROUP_BY.getFieldName())) {
                                groupBy = originObj.getBoolean(GraphQueryKeys.GRAPH_QUERY_GROUP_BY.getFieldName());
                            }
                            if (originObj.has(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName())) {
                                fieldFilter = createFieldFilter(originObj.getJSONObject(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName()));
                            }

                            Map<String, Object> customDirectives = new LinkedHashMap<>();
                            Iterator keys = originObj.keys();
                            while(keys.hasNext()){
                                Object key = keys.next();
                                if(key instanceof String && !GraphQueryKeys.isKey((String)key)){
                                    customDirectives.put((String)key, originObj.get((String)key));
                                }
                            }
                            fieldsPerRelativePath.add(new SpecField(fieldName, specFields, traversalPath, groupedInstances, required, sortAlphabetically, groupBy, ensureOrder, fieldFilter, customDirectives));
                        }
                    }
                }
                if (fieldsPerRelativePath.size() > 1) {
                    SpecField rootField = null;
                    for (int i = 0; i < fieldsPerRelativePath.size(); i++) {
                        SpecField specField = fieldsPerRelativePath.get(i);
                        if (rootField == null) {
                            rootField = new SpecField(specField.fieldName, fieldsPerRelativePath, Collections.emptyList(), specField.groupedInstances, specField.required, specField.sortAlphabetically, specField.groupby, specField.ensureOrder, specField.fieldFilter);
                        }
                        specField.sortAlphabetically = false;
                        specField.groupby = false;
                        specField.required = false;
                        specField.fieldName = String.format("%s_%d", specField.fieldName, i);
                    }
                    return Collections.singletonList(rootField);
                } else if (!fieldsPerRelativePath.isEmpty()) {
                    return Collections.singletonList(fieldsPerRelativePath.get(0));
                }

            }
        }
        return result;
    }

    private Object removeAtId(Object object) throws JSONException {
        if (object instanceof JSONObject && ((JSONObject) object).has(JsonLdConsts.ID)) {
            return ((JSONObject) object).get(JsonLdConsts.ID);
        }
        return object;

    }

    private boolean hasMultipleRelativePaths(JSONArray relativePath) throws JSONException {
        for (int i = 0; i < relativePath.length(); i++) {
            if (relativePath.get(i) instanceof JSONArray) {
                return true;
            }
        }
        return false;
    }


    private List<Object> getAllRelativePaths(Object merge) throws JSONException {
        if (merge instanceof JSONArray) {
            JSONArray mergeArray = (JSONArray) merge;
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < mergeArray.length(); i++) {
                if (mergeArray.get(i) instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) mergeArray.get(i);
                    if (jsonObject.has(GraphQueryKeys.GRAPH_QUERY_RELATIVE_PATH.getFieldName())) {
                        result.add(jsonObject.get(GraphQueryKeys.GRAPH_QUERY_RELATIVE_PATH.getFieldName()));
                    }
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }


    private List<SpecTraverse> createTraversalPath(Object relativePath) throws JSONException {
        List<SpecTraverse> result = new ArrayList<>();
        if (relativePath instanceof JSONArray) {
            JSONArray relativePathArray = (JSONArray) relativePath;
            for (int i = 0; i < relativePathArray.length(); i++) {
                Object relativePathElement = relativePathArray.get(i);
                if (relativePathElement != null) {
                    result.add(createSpecTraverse(relativePathElement));
                }
            }
        } else {
            result.add(createSpecTraverse(relativePath));
        }
        return result;
    }

    private SpecTraverse createSpecTraverse(Object relativePathElement) throws JSONException {
        String path = null;
        boolean reverse = false;

        if (relativePathElement instanceof JSONObject && ((JSONObject) relativePathElement).has(JsonLdConsts.ID)) {
            path = ((JSONObject) relativePathElement).getString(JsonLdConsts.ID);
            if (((JSONObject) relativePathElement).has(GraphQueryKeys.GRAPH_QUERY_REVERSE.getFieldName())) {
                reverse = ((JSONObject) relativePathElement).getBoolean(GraphQueryKeys.GRAPH_QUERY_REVERSE.getFieldName());
            }
        } else {
            path = relativePathElement.toString();
        }
        return new SpecTraverse(path, reverse);
    }


    public static FieldFilter createFieldFilter(JSONObject fieldFilter) throws JSONException {
        if (fieldFilter.has(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName())) {
            String stringOp = fieldFilter.getString(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName());
            if (stringOp != null) {
                Op op = Op.valueOf(stringOp.toUpperCase());
                Value value = null;
                Parameter parameter = null;
                if (fieldFilter.has(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName())) {
                    String stringValue = fieldFilter.getString(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName());
                    if (stringValue != null) {
                        value = new Value(stringValue);
                    }
                }
                if (fieldFilter.has(GraphQueryKeys.GRAPH_QUERY_FILTER_PARAM.getFieldName())) {
                    String stringParameter = fieldFilter.getString(GraphQueryKeys.GRAPH_QUERY_FILTER_PARAM.getFieldName());
                    if (stringParameter != null) {
                        parameter = new Parameter(stringParameter);
                    }
                }
                return new FieldFilter(op, value, parameter);
            }
        }
        return null;

    }
}
