package org.humanbrainproject.knowledgegraph.commons.api;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.ExampleValues;

@NoTests(NoTests.NO_LOGIC)
public class ParameterConstants {

    public static final String TEMPLATE_ID = "templateId";
    public static final String QUERY_ID = "queryId";
    public static final String ORG = "org";
    public static final String DOMAIN = "domain";
    public static final String LIBRARY = "library";
    public static final String VOCAB = "vocab";
    public static final String START = "start";
    public static final String SIZE = "size";
    public static final String ORGS = "orgs";
    public static final String DATABASE_SCOPE = "databaseScope";
    public static final String SEARCH = "search";
    public static final String SCHEMA = "schema";
    public static final String VERSION = "version";
    public static final String INSTANCE_ID = "instanceId";
    public static final String RESTRICT_TO_ORGANIZATIONS = "restrictToOrganizations";
    public static final String CLIENT_ID_EXTENSION = "clientIdExtension";
    public static final String CLIENT = "client";
    public static final String ID ="id";
    public static final String REV ="rev";

    public static final String DATABASE_SCOPE_DOC = "Defines the database scope. This is not taken into account if a client extension is defined (can only come from the NATIVE space)";
    public static final String CLIENT_EXTENSION_DOC = "The clientIdExtension allows the calling client to specify an additional postfix to the identifier and therefore to discriminate between different instances which are combined in the inferred space. If this value takes a userId for example, this means that there will be a distinct instance created for every user.";
    public static final String AUTHORIZATION_DOC = "Your authorization token (either with a trailing \"Bearer \" or without)";
    public static final String VOCAB_DOC="A namespace which shall be treated as the @vocab (the default namespace). If defined, the keys matching this namespace will be simpliniified)";
    public static final String SIZE_DOC = "For pagination: Defines the maximal size of the queries page";
    public static final String START_DOC = "For pagination: Defines the initial offset of the pagination (0-based)";
    public static final String RESTRICTED_ORGANIZATION_DOC = "Restrict the results to explicitly defined organizations - the main use case is if a user with broad access permissions wants to simulate restricted read access (e.g. the indexing functionality of the KG search UI)";
    public static final String SEARCH_DOC = "A search string checking for instances with names which match this term (includes wildcards)";

    public static final String BOUNDING_BOX_DOC = "A minimal bounding box - if defined, only results which are spatially anchored and are part of the given region are returned. Follow the pattern \""+ ExampleValues.MBB_EXAMPLE+"\"";

}
