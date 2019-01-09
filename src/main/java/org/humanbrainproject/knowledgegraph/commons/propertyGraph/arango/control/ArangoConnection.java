package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;
import java.util.stream.Collectors;

@ToBeTested(systemTestRequired = true)
public class ArangoConnection implements DatabaseConnection<ArangoDatabase>{

    @Value("${org.humanbrainproject.knowledgegraph.arango.host}")
    String host;
    @Value("${org.humanbrainproject.knowledgegraph.arango.port}")
    Integer port;
    @Value("${org.humanbrainproject.knowledgegraph.arango.user}")
    String user;
    @Value("${org.humanbrainproject.knowledgegraph.arango.pwd}")
    String pwd;

    private final String databaseName;
    private final boolean translateToMainSpace;

    ArangoDB arangoDB;

    public ArangoConnection(String databaseName, boolean translateToMainSpace) {
        this.databaseName = databaseName;
        this.translateToMainSpace = translateToMainSpace;
    }

    private ArangoDB getArangoDB(){
        if(arangoDB==null){
            arangoDB = new ArangoDB.Builder().host(host, port).user(user).password(pwd).build();
        }
        return arangoDB;
    }

    public Set<ArangoCollectionReference> getEdgesCollectionNames(){
        return getOrCreateDB().getCollections().stream().filter(c -> !c.getIsSystem() && c.getType() == CollectionType.EDGES).map(c -> new ArangoCollectionReference(c.getName())).collect(Collectors.toSet());
    }

    public ArangoDatabase getOrCreateDB() {
        return getOrCreateDB(true);
    }

    private ArangoDatabase getOrCreateDB(boolean retryOnFail){
        try {
            ArangoDatabase kg = getArangoDB().db(databaseName);
            if (!kg.exists()) {
                kg.create();
            }
            return kg;
        }
        catch(ArangoDBException exception){
            if(retryOnFail) {
                arangoDB = null;
                return getOrCreateDB(false);
            }
            else{
                throw  exception;
            }
        }
    }

    public Set<ArangoCollectionReference> getCollections(){
        return getOrCreateDB().getCollections().stream().map(r -> new ArangoCollectionReference(r.getName())).collect(Collectors.toSet());
    }


    public Set<ArangoCollectionReference> filterExistingCollectionLabels(Set<ArangoCollectionReference> collections){
        Set<ArangoCollectionReference> existingCollectionLabels = getCollections();
        return collections.stream().filter(existingCollectionLabels::contains).collect(Collectors.toSet());
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public boolean isTranslateToMainSpace() {
        return translateToMainSpace;
    }

    @Override
    public void clearData() {
        ArangoDatabase db = getOrCreateDB();
        for (CollectionEntity collectionEntity : db.getCollections()) {
            if (!collectionEntity.getName().startsWith("_")) {
                db.collection(collectionEntity.getName()).drop();
            }
        }
    }
}
