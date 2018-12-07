package org.humanbrainproject.knowledgegraph.structure.api;

import org.humanbrainproject.knowledgegraph.commons.InternalApi;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.structure.boundary.Structure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;

@InternalApi
@RestController
@RequestMapping(value = "/structure", produces = MediaType.APPLICATION_JSON)
public class StructureAPI {

    @Autowired
    Structure structure;

    @GetMapping
    public JsonDocument getStructure(){
        return structure.getStructure();
    }


    @GetMapping("/bySpec")
    public void getStructureBySpecification(){
        structure.reflectOnSpecifications();
    }

}
