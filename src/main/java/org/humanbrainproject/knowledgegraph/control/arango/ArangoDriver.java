package org.humanbrainproject.knowledgegraph.control.arango;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(scopeName = "singleton")
public class ArangoDriver {

    @Value("${org.humanbrainproject.knowledgegraph.arango.host}")
    String host;
    @Value("${org.humanbrainproject.knowledgegraph.arango.port}")
    Integer port;
    @Value("${org.humanbrainproject.knowledgegraph.arango.user}")
    String user;
    @Value("${org.humanbrainproject.knowledgegraph.arango.pwd}")
    String pwd;

    ArangoDB arangoDB;

    private static final String DATABASE_NAME="kg";

    private ArangoDB getArangoDB(){
        if(arangoDB==null){
            arangoDB = new ArangoDB.Builder().host(host, port).user(user).password(pwd).build();
        }
        return arangoDB;
    }

    public ArangoDatabase getOrCreateDB(){
        ArangoDatabase kg = getArangoDB().db(DATABASE_NAME);
        if(!kg.exists()){
            kg.create();
        }
        return kg;
    }
}
