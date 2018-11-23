package org.humanbrainproject.knowledgegraph;

import com.google.common.base.Predicates;
import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket publicApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("0_public")
                .select()
                .apis(Predicates.not(RequestHandlerSelectors.withClassAnnotation(InternalApi.class)))
                .paths(PathSelectors.regex("^(?!/error).*"))
                .build();
    }



    @Bean
    public Docket internalApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("1_internal")
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(InternalApi.class))
                .paths(PathSelectors.regex("^(?!/error).*"))
                        .build();
    }


}
