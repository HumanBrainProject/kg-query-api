package org.humanbrainproject.knowledgegraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.humanbrainproject.knowledgegraph")
public class KgQueryApplication {

	public static void main(String[] args) {
		SpringApplication.run(KgQueryApplication.class, args);
	}

}
