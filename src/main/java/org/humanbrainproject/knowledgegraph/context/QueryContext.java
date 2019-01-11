package org.humanbrainproject.knowledgegraph.context;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.ExposedDatabaseScope;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
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

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    private final Map<String, Object> allParameters = new HashMap<>();
    private DatabaseScope databaseScope = DatabaseScope.INFERRED;


    public DatabaseScope getDatabaseScope() {
        return databaseScope;
    }

    public Map<String, Object> getAllParameters() {
        return allParameters;
    }

    public void setAllParameters(Map<String, Object> parameters) {
        allParameters.putAll(parameters);
    }

    public void setDatabaseScope(DatabaseScope databaseScope) {
        this.databaseScope = databaseScope == null ? DatabaseScope.INFERRED : null;
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


    public <T> ArangoCursor<T> queryDatabase(String aqlQuery, boolean count, Pagination pagination, Class<T> returnType) {
        AqlQueryOptions options = new AqlQueryOptions();
        if (count) {
            if (pagination != null) {
                options.fullCount(true);
            } else {
                options.count(true);
            }
        }
        return getDatabase().query(aqlQuery, null, options, returnType);
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
