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

package org.humanbrainproject.knowledgegraph.context;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.ExposedDatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.releasing.control.ReleaseControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * This is a stateful bean (with request scope) which holds generic query information (e.g. the database scope). Population of the values happens as part of the API declaration -> (see e.g. {@link org.humanbrainproject.knowledgegraph.query.api.QueryAPI}).
 */
@Component
@RequestScope
@ToBeTested(easy = true)
public class QueryContext {


    protected Logger logger = LoggerFactory.getLogger(QueryContext.class);
    @Autowired
    ArangoDatabaseFactory databaseFactory;

    private final Map<String, String> allParameters = new HashMap<>();
    private DatabaseScope databaseScope = DatabaseScope.INFERRED;


    public DatabaseScope getDatabaseScope() {
        return databaseScope;
    }

    public Map<String, String> getAllParameters() {
        return allParameters;
    }

    public void setAllParameters(Map<String, String> parameters) {
        allParameters.putAll(parameters);
    }

    public void setDatabaseScope(DatabaseScope databaseScope) {
        this.databaseScope = databaseScope == null ? DatabaseScope.INFERRED : databaseScope;
    }

    public ArangoConnection getDatabaseConnection() {
        return databaseFactory.getConnection(getDatabaseScope());
    }

    public ArangoDatabase getDatabase() {
        return getDatabaseConnection().getOrCreateDB();
    }

    public ArangoConnection getDatabaseConnectionByExplicitScope(DatabaseScope databaseScope){
        return databaseFactory.getConnection(databaseScope);
    }

    public ArangoDatabase getDatabaseByExplicitScope(DatabaseScope databaseScope){
        return getDatabaseConnectionByExplicitScope(databaseScope).getOrCreateDB();
    }


    public <T> ArangoCursor<T> queryDatabase(String aqlQuery, boolean count, Pagination pagination, Class<T> returnType, Map<String, Object> bindParameters) {
        AqlQueryOptions options = new AqlQueryOptions();
        if (count) {
            if (pagination != null && pagination.getSize()!=null) {
                options.fullCount(true);
            } else {
                options.count(true);
            }
        }
        try {
            return getDatabase().query(aqlQuery, bindParameters, options, returnType);
        }
        catch(ArangoDBException ex){
            logger.error(String.format("Was not able to execute the query: \n%s", aqlQuery), ex);
            throw ex;
        }
    }

    private transient Set<ArangoCollectionReference> existingCollections;


    public Set<ArangoCollectionReference> getExistingCollections() {
        if (existingCollections == null) {
            existingCollections = getDatabaseConnection().getCollections();
        }
        return existingCollections;
    }

    public void populateQueryContext(ExposedDatabaseScope databaseScope) {
        if (databaseScope != null) {
            setDatabaseScope(databaseScope.toDatabaseScope());
        }
    }

}
