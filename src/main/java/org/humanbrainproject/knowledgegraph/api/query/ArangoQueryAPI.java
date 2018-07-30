package org.humanbrainproject.knowledgegraph.api.query;

import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(value = "/arango", consumes = {MediaType.APPLICATION_JSON, "application/ld+json"}, produces = MediaType.APPLICATION_JSON)
public class ArangoQueryAPI implements KGQueryAPI {

    @Autowired
    ArangoQuery query;

    @Override
    @PostMapping("/query")
    public List<Object> queryPropertyGraphBySpecification(@RequestBody String payload) throws Exception {
        return query.queryPropertyGraphBySpecification(payload);
    }

    @PostMapping("/query/{id}")
    public void saveSpecificationToDB(@RequestBody String payload, @PathVariable("id") String id) throws Exception {
        query.storeSpecificationInDb(payload, id);
    }

    @GetMapping("/query/{id}")
    public List<Object> executeStoredQuery(@PathVariable("id") String id) throws Exception {
        return query.queryPropertyGraphByStoredSpecification(id);
    }


    @PostMapping(value = "/query/{id}/templates", consumes = {MediaType.TEXT_PLAIN})
    public List<Object> applyTemplateToApi(@RequestBody String template, @PathVariable("id") String id) throws Exception {
        return query.queryPropertyGraphByStoredSpecificationAndTemplate(id, template);
    }

}
