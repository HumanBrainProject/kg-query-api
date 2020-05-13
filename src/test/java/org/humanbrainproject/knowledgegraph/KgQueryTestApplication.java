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
