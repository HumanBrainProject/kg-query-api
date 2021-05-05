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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.IllegalDatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@NoTests(NoTests.TRIVIAL)
public class ArangoDatabaseFactory {

    @Autowired
    @Qualifier("released")
    ArangoConnection releasedDB;

    @Autowired
    @Qualifier("default")
    ArangoConnection defaultDB;

    @Autowired
    @Qualifier("inferred")
    ArangoConnection inferredDB;

    @Autowired
    @Qualifier("internal")
    ArangoConnection arangoInternal;

    @Autowired
    AuthorizationContext authorizationContext;

    public ArangoConnection getReleasedDB() {
        return releasedDB;
    }

    public ArangoConnection getDefaultDB(boolean asSystemUser) {
        if(asSystemUser || !authorizationContext.isAllowedToSeeReleasedInstancesOnly()){
            return defaultDB;
        }
        throw new IllegalDatabaseScope("You've tried to read from the default space - this is not allowed with your token.");
    }

    public ArangoConnection getInferredDB(boolean asSystemUser) {
        if(asSystemUser || !authorizationContext.isAllowedToSeeReleasedInstancesOnly()){
            return inferredDB;
        }
        throw new IllegalDatabaseScope("You've tried to read from the curated space - this is not allowed with your token.");
    }

    public ArangoConnection getInternalDB() {
        return arangoInternal;
    }


    public ArangoConnection getConnection(DatabaseScope scope) {
        switch (scope) {
            case NATIVE:
                return getDefaultDB(false);
            case RELEASED:
                return getReleasedDB();
            case INFERRED:
                return getInferredDB(false);
        }
        return getDefaultDB(false);
    }

}
