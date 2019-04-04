package org.humanbrainproject.knowledgegraph.eventReader;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.SystemOidcClient;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Component
public class EventReader {

    @Autowired
    JsonTransformer jsonTransformer;

    private String lastEventId;
    Path path = Paths.get("/tmp/lastEvent");
    WebClient client;

    @Autowired
    SystemOidcClient oidcClient;

    @PostConstruct
    public void init() {
        if (Files.exists(path)) {
            try {
                lastEventId = new String(Files.readAllBytes(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        client = WebClient.create("https://nexus-ppd.humanbrainproject.org/v1");
        recursiveSubscription();
    }

    private void consume(ServerSentEvent event) {
        try {
            lastEventId = event.id();
            Files.write(Paths.get("/tmp/lastEvent"), lastEventId.getBytes());
            System.out.println(event);
        } catch (Exception e) {

        }
    }

    private void waitAndReconnectAfterException(Throwable e){
        e.printStackTrace();
        waitAndReconnect();
    }

    private void waitAndReconnect(){
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        recursiveSubscription();
    }


    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
        if(ex.getStatusCode()==HttpStatus.UNAUTHORIZED){
            System.out.println("Token timed out - refresh");
            oidcClient.refreshToken();
            waitAndReconnect();
        }
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
    }


    private void recursiveSubscription() {
        WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = client.get();
        if (lastEventId != null) {
            requestHeadersUriSpec.header("Last-Event-ID", lastEventId);
        }
        try{
            Flux<ServerSentEvent> events = requestHeadersUriSpec.uri("/kgevents").header(HttpHeaders.AUTHORIZATION, oidcClient.getAuthorizationToken().getBearerToken()).accept(MediaType.TEXT_EVENT_STREAM).retrieve().bodyToFlux(ServerSentEvent.class);
            events.log().timeout(Duration.of(5, ChronoUnit.MINUTES)).subscribe(this::consume, this::waitAndReconnectAfterException, this::waitAndReconnect);
        }
        catch (WebClientResponseException e){
            handleWebClientResponseException(e);
        }
    }

}
