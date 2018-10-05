package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;
import java.util.stream.Collectors;

public class ArangoDriver {

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

    public ArangoDriver(String databaseName, boolean translateToMainSpace) {
        this.databaseName = databaseName;
        this.translateToMainSpace = translateToMainSpace;
    }

    private ArangoDB getArangoDB(){
        if(arangoDB==null){
            arangoDB = new ArangoDB.Builder().host(host, port).user(user).password(pwd).build();
        }
        return arangoDB;
    }

    public Set<String> getEdgesCollectionNames(){
        return getOrCreateDB().getCollections().stream().filter(c -> !c.getIsSystem() && c.getType() == CollectionType.EDGES).map(CollectionEntity::getName).collect(Collectors.toSet());
    }


    public ArangoDatabase getOrCreateDB(){
        ArangoDatabase kg = getArangoDB().db(databaseName);
        if(!kg.exists()){
            kg.create();
        }
        return kg;
    }

    public Set<String> getCollectionLabels() {
        return getOrCreateDB().getCollections().stream().map(CollectionEntity::getName).collect(Collectors.toSet());
    }

    public Set<String> filterExistingCollectionLabels(Set<String> collectionLabels){
        Set<String> existingCollectionLabels = getCollectionLabels();
        return collectionLabels.stream().filter(existingCollectionLabels::contains).collect(Collectors.toSet());
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public boolean isTranslateToMainSpace() {
        return translateToMainSpace;
    }
}
