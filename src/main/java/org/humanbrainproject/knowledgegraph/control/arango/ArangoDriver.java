package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

public abstract class ArangoDriver {

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

    public ArangoDatabase getOrCreateDB(){
        ArangoDatabase kg = getArangoDB().db(databaseName);
        if(!kg.exists()){
            kg.create();
        }
        return kg;
    }
}
