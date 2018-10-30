package deprecated.control;

import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;

public enum GraphQueryKeys {


    GRAPH_QUERY_SPECIFICATION ("specification"),
    GRAPH_QUERY_ROOT_SCHEMA ("root_schema"),
    GRAPH_QUERY_FIELDNAME("fieldname"),
    GRAPH_QUERY_RELATIVE_PATH("relative_path"),
    GRAPH_QUERY_FIELDS("fields"),
    GRAPH_QUERY_REQUIRED("required"),
    GRAPH_QUERY_REVERSE("reverse"),
    GRAPH_QUERY_MERGE("merge"),
    GRAPH_QUERY_SORT("sort"),
    GRAPH_QUERY_GROUP_BY("group_by"),
    GRAPH_QUERY_ENSURE_ORDER("ensure_order"),
    GRAPH_QUERY_GROUPED_INSTANCES("grouped_instances"),
    GRAPH_QUERY_GROUPED_INSTANCES_DEFAULT("instances"),
    GRAPH_QUERY_ARANGO_ID("_id"),
    GRAPH_QUERY_ARANGO_REV("_rev"),
    GRAPH_QUERY_ARANGO_KEY("_key");

    private final String fieldName;

    GraphQueryKeys(String fieldName){
        this.fieldName = HBPVocabulary.GRAPH_QUERY+fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
