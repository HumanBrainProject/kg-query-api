package org.humanbrainproject.knowledgegraph.eventReader;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
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


    private void recursiveSubscription() {
        WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = client.get();
        if (lastEventId != null) {
            requestHeadersUriSpec.header("Last-Event-ID", lastEventId);
        }
        Flux<ServerSentEvent> events = requestHeadersUriSpec.uri("/kgevents").accept(MediaType.TEXT_EVENT_STREAM).retrieve().bodyToFlux(ServerSentEvent.class);
        events.log().timeout(Duration.of(5, ChronoUnit.MINUTES)).subscribe(this::consume, this::waitAndReconnectAfterException, this::waitAndReconnect);
    }

}
