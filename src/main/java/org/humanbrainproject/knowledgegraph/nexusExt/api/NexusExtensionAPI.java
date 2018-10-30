package org.humanbrainproject.knowledgegraph.nexusExt.api;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(value = "/nexus", produces = MediaType.APPLICATION_JSON)
@Api(value="/nexus", description = "The extension API for managing resources on Nexus")
public class NexusExtensionAPI {


}
