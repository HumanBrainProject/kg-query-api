package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

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

    ArangoDB arangoDB;

    public ArangoDriver(String databaseName) {
        this.databaseName = databaseName;
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
}
