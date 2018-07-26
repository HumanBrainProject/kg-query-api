package org.humanbrainproject.knowledgegraph.api.echo;

import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping(value = "/echo")
public class EchoAPI {

    protected Logger log = Logger.getLogger(EchoAPI.class.getName());

    @PostMapping
    @PutMapping
    @DeleteMapping
    @PatchMapping
    public String echo(@RequestBody String request){
        log.info(String.format("Echoing %s", request));
        return request;
    }

    @GetMapping
    public String echo(){
        log.info("Echoing to GET");
        return "Hello world";
    }

}
