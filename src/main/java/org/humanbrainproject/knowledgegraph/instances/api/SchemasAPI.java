package org.humanbrainproject.knowledgegraph.instances.api;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(value = "/api/schemas", produces = MediaType.APPLICATION_JSON)
@Api(value = "/api/schemas", description = "The API for managing schemas")
@NoTests(NoTests.NO_LOGIC)
public class SchemasAPI {

}

