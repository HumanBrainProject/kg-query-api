package org.humanbrainproject.knowledgegraph;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseTransaction;
import org.humanbrainproject.knowledgegraph.commons.solr.Solr;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;


@Configuration
@Profile("test")
public class KgQueryTestApplication {


    @Bean
    @Primary
    public DatabaseTransaction databaseTransaction(){
        return Mockito.mock(DatabaseTransaction.class);
    }


    @Bean
    @Primary
    public NexusClient nexusClient() {
        return new NexusMockClient();
    }

    @Bean
    @Primary
    public AuthorizationContext authorizationContext(){
        return new TestAuthorizationContext();
    }

    @Bean
    @Primary
    public Solr solr(){
        return Mockito.mock(Solr.class);
    }

}
