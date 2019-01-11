package org.humanbrainproject.knowledgegraph;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan("org.humanbrainproject.knowledgegraph")
@NoTests(NoTests.NO_LOGIC)
public class KgQueryApplication {

	public static void main(String[] args) {
		SpringApplication.run(KgQueryApplication.class, args);
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

}
