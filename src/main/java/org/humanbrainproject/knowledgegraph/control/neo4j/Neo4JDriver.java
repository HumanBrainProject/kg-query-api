package org.humanbrainproject.knowledgegraph.control.neo4j;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(scopeName = "singleton")
public class Neo4JDriver {

    @Value("${org.humanbrainproject.knowledgegraph.neo4j.uri}")
    String uri;

    @Value("${org.humanbrainproject.knowledgegraph.neo4j.user}")
    String user;

    @Value("${org.humanbrainproject.knowledgegraph.neo4j.pwd}")
    String pwd;


    private Driver driver;

    public Driver getDriver(){
        if(driver==null){
            driver =GraphDatabase.driver(uri, AuthTokens.basic(user, pwd));
        }
        return driver;

    }

}
