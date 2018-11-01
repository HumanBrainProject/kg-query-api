package org.humanbrainproject.knowledgegraph.commons.vocabulary;

public class HBPVocabulary {

    public static final String ALIAS = "hbp";
    public static final String NAMESPACE = "https://schema.hbp.eu/";

    public static final String GRAPH_QUERY = NAMESPACE+"graph_query/";

    // FOR PROVENANCE
    private static final String PROVENANCE = NAMESPACE+"provenance/";
    public static final String PROVENANCE_MODIFIED_AT = PROVENANCE + "modifiedAt";
    public static final String PROVENANCE_INDEXED_IN_ARANGO_AT = PROVENANCE + "indexedInArangoAt";
    public static final String PROVENANCE_LAST_MODIFICATION_USER_ID = PROVENANCE + "lastModificationUserId";

    // FOR RELEASING
    public static final String RELEASE_TYPE = HBPVocabulary.NAMESPACE + "Release";
    public static final String RELEASE_INSTANCE_PROPERTYNAME = RELEASE_TYPE.toLowerCase() + "/instance";
    public static final String RELEASE_REVISION_PROPERTYNAME = RELEASE_TYPE.toLowerCase() + "/revision";

    //FOR INFERENCE
    public final static String INFERENCE_TYPE = HBPVocabulary.NAMESPACE + "Inference";
    public final static String INFERENCE_SOURCE = INFERENCE_TYPE.toLowerCase() + "/source";
    public final static String INFERENCE_OF_PROPERTY = INFERENCE_TYPE.toLowerCase()+"/inferenceOf";


    /**
     * declares the relationship of e.g. an editor instance which extends another (original) entity
     */
    public final static String INFERENCE_EXTENDS = INFERENCE_TYPE.toLowerCase()+"/extends";
    public final static String INFERENCE_ALTERNATIVES = INFERENCE_TYPE.toLowerCase()+"/alternatives";

}
