package org.humanbrainproject.knowledgegraph.api.echo;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/echo")
public class EchoAPI {

    @GetMapping
    @PostMapping
    @PutMapping
    @DeleteMapping
    @PatchMapping
    public String echo(@RequestBody String request){
        return request;
    }

}
