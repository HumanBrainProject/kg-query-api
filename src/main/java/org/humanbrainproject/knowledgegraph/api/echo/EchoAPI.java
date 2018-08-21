package org.humanbrainproject.knowledgegraph.api.echo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/kg/echo")
public class EchoAPI {

    Logger logger = LoggerFactory.getLogger(EchoAPI.class);


    @PostMapping
    @PutMapping
    @DeleteMapping
    @PatchMapping
    public String echo(@RequestBody String request){
        logger.info("Echoing {}", request);
        return request;
    }

    @GetMapping
    public String echo(){
        logger.info("Echoing to GET");
        return "Hello world";
    }

}
