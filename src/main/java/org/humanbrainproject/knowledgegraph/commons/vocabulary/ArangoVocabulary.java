package org.humanbrainproject.knowledgegraph.commons.vocabulary;


import org.humanbrainproject.knowledgegraph.annotations.NoTests;

@NoTests(NoTests.NO_LOGIC)
public class ArangoVocabulary {

    //Arango owned
    public static final String ID = "_id";
    public static final String KEY = "_key";
    public static final String REV = "_rev";

    //General custom
    public static final String NAME = "_name";
    public static final String PERMISSION_GROUP = "_permissionGroup";


    //Relations
    public static final String FROM = "_from";
    public static final String TO = "_to";
    public static final String ORDER_NUMBER = "_orderNumber";
    public static final String PATH = "_path";
    public static final String NEXT = "_next";


    //Backreference to Nexus
    public static final String NEXUS_REV ="_nexusRev";
    public static final String NEXUS_UUID = "_nexusUUID";
    public static final String NEXUS_RELATIVE_URL = "_nexusRelativeUrl";
    public static final String NEXUS_RELATIVE_URL_WITH_REV = "_nexusRelativeUrlWithRev";


    //Lookup
    public static final String LOOKUP_COLLECTION = "lookup";
    public static final String LOOKUP_SCHEMAS = "lookup_schemas";


    public static final String INDEXED_IN_ARANGO_AT = "_indexedInArangoAt";

    public static final String LIBRARY = "library";

    //TODO change value and re-index
    //public static final String NEXUS_RELATIVE_URL_WITH_REV = "_originalId";


}
