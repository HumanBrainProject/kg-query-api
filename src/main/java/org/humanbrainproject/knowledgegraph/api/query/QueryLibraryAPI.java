package org.humanbrainproject.knowledgegraph.api.query;

import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.humanbrainproject.knowledgegraph.boundary.query.Templating;
import org.humanbrainproject.knowledgegraph.entity.Template;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping(value = "/libraries", produces = MediaType.APPLICATION_JSON)
@Api(value="/libraries", description = "The API for managing template libraries")
public class QueryLibraryAPI {

    @Autowired
    ArangoQuery query;

    @Autowired
    Templating templating;

    @PutMapping(value = "/{libraryId}", consumes = {MediaType.TEXT_PLAIN})
    public ResponseEntity<Void> saveFreemarkerLibrary(@RequestBody String template, @PathVariable("libraryId") String libraryId) throws Exception {
        Template t = new Template();
        t.setTemplateContent(template);
        t.set_key(libraryId);
        templating.saveLibrary(t);
        return null;
    }


}
