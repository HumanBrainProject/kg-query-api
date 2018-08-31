package org.humanbrainproject.knowledgegraph.api.query;

import com.arangodb.entity.AqlFunctionEntity;
import io.swagger.annotations.Api;
import org.humanbrainproject.knowledgegraph.boundary.graph.ArangoGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@RestController
@RequestMapping(value = "/arango", produces = MediaType.APPLICATION_JSON)
@Api(value="/arango/graph", description = "The API for reflecting the knowledge graph")
public class GraphAPI {

    @Autowired
    ArangoGraph graph;

    @GetMapping(value = "/graph/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<List<Map>> getGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id, @RequestParam(value= "step", required = false, defaultValue = "2") Integer step) throws Exception{
        try{
            String v = version.replaceAll("\\.", "_");
            String vert =  String.format("%s-%s-%s-%s/%s", org,domain, schema, v, id);
            //TODO Validate step value
            return ResponseEntity.ok(graph.getGraph(vert, step));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping(value = "/release/{org}/{domain}/{schema}/{version}/{id}", consumes = { MediaType.WILDCARD})
    public ResponseEntity<List<Map>> getReleaseGraph(@PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @PathVariable("id") String id) throws Exception{
        try{
            String v = version.replaceAll("\\.", "_");
            String vert =  String.format("%s-%s-%s-%s/%s", org,domain, schema, v, id);
            String functionNamespace = "GO::LOCATED_IN";
            String functionName = "APPEND_CHILD_STRUCTURE";
            String function = "function (root, flatStructure) {\n" +
                    "                if (root && root.id) {\n" +
                    "                    var elsById = {};\n" +
                    "                    elsById[root.id] = root;\n" +
                    "                    flatStructure.forEach(function (element) {\n" +
                    "                        elsById[element.id] = element;\n" +
                    "                        var parentElId = element.children[element.children.length - 2];\n" +
                    "                        var parentEl = elsById[parentElId];\n" +
                    "                        if(parentEl){\n" +
                    "                           if (!parentEl.children)\n" +
                    "                               parentEl.children = new Array();\n" +
                    "                           parentEl.children.push(element);\n" +
                    "                         }\n" +
                    "                        delete element.children;\n" +
                    "                    });\n" +
                    "                }\n" +
                    "                return root;\n" +
                    "            }";
            graph.uploadFunction(String.format("%s::%s", functionNamespace, functionName), function);
            return ResponseEntity.ok(graph.getReleaseGraph(vert));
        } catch (HttpClientErrorException e){
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }


}
