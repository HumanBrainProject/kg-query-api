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

package org.humanbrainproject.knowledgegraph.commons.api;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.ExampleValues;

@NoTests(NoTests.NO_LOGIC)
public class ParameterConstants {

    public static final String TEMPLATE_ID = "templateId";
    public static final String EDITOR_SPEFICATION_ID = "editorSpecId";
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
