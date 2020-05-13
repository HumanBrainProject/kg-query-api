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

import akka.actor.ActorSystem;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.request.RequestContextListener;


@SpringBootApplication
@ComponentScan("org.humanbrainproject.knowledgegraph")
@NoTests(NoTests.NO_LOGIC)
@EnableCaching
@EnableScheduling
@EnableAsync
public class KgQueryApplication {

	public static void main(String[] args) {
		SpringApplication.run(KgQueryApplication.class, args);
	}

	@Bean public RequestContextListener requestContextListener(){
		return new RequestContextListener();
	}

	@Bean
	@Qualifier("default-test")
	public ArangoConnection createDefaultTestDb() {
		return new ArangoConnection("kg-test", false);
	}

	@Bean
	@Qualifier("internal-test")
	public ArangoConnection createInternalTestDb() {
		return new ArangoConnection("kg_internal", false);
	}

	@Bean
	@Qualifier("released-test")
	public ArangoConnection createReleasedTestDb() {
		return new ArangoConnection("kg_released-test", false);
	}

	@Bean
	@Qualifier("default")
	public ArangoConnection createDefaultDb() {
		return new ArangoConnection("kg", false);
	}

	@Bean
	@Qualifier("inferred")
	public ArangoConnection createInferredDb() {
		return new ArangoConnection("kg_inferred", true);
	}

	@Bean
	@Qualifier("playground")
	public ArangoConnection createPlaygroundDb() {
		return new ArangoConnection("kg_playground", false);
	}

	@Bean
	@Qualifier("playground-released")
	public ArangoConnection createPlaygroundReleasedDb() {
		return new ArangoConnection("kg_playground_released", false);
	}

	@Bean
	@Qualifier("internal")
	public ArangoConnection createInternalDb() {
		return new ArangoConnection("kg_internal", false);
	}

	@Bean
	@Qualifier("released")
	public ArangoConnection createReleasedDb() {
		return new ArangoConnection("kg_released", false);
	}

	@Bean
	public ActorSystem actorSystem() {
		ActorSystem system = ActorSystem.create("uploader-actor-system");
		return system;
	}


}
