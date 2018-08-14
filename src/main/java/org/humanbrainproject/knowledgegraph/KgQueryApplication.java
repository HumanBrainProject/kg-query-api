package org.humanbrainproject.knowledgegraph;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.humanbrainproject.knowledgegraph")
public class KgQueryApplication {

	public static void main(String[] args) {
		SpringApplication.run(KgQueryApplication.class, args);
	}


	@Bean
	@Qualifier("default-test")
	public ArangoDriver createDefaultTestDb() {
		return new ArangoDriver("kg-test");
	}

	@Bean
	@Qualifier("internal-test")
	public ArangoDriver createInternalTestDb() {
		return new ArangoDriver("kg_internal");
	}

	@Bean
	@Qualifier("released-test")
	public ArangoDriver createReleasedTestDb() {
		return new ArangoDriver("kg_released-test");
	}

	@Bean
	@Qualifier("default")
	public ArangoDriver createDefaultDb() {
		return new ArangoDriver("kg");
	}

	@Bean
	@Qualifier("internal")
	public ArangoDriver createInternalDb() {
		return new ArangoDriver("kg_internal");
	}

	@Bean
	@Qualifier("released")
	public ArangoDriver createReleasedDb() {
		return new ArangoDriver("kg_released");
	}
}
