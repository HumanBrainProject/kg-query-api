package org.humanbrainproject.knowledgegraph.nexus.entity;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.CleanDataMsg;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.GetStatusMsg;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.UploadDataMsg;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.UploadDoneMsg;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class UploadActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final FileStructureUploader uploader;
    private ActorRef parent;

    static Props props(FileStructureUploader uploader, ActorRef parent) {
        return Props.create(UploadActor.class, () -> new UploadActor(uploader, parent
        ));
    }

    public UploadActor(FileStructureUploader uploader, ActorRef parent){
        this.uploader = uploader;
        this.parent = parent;
    }

    public void uploadData(){
        try{
            this.uploader.uploadData();
        }catch (IOException e){
            e.printStackTrace();
            log.error("Error: " + e.getMessage());
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        UploadDataMsg.class,
                        s -> {
                            log.info("Upload data message received");
                            CompletableFuture.runAsync(this::uploadData).thenRun( () -> this.parent.tell(new UploadDoneMsg(s.getId()), self()) );
                        })
                .match(
                        GetStatusMsg.class,
                        s -> sender().tell(this.uploader.getStatus(), self())
                )
                .match(
                        CleanDataMsg.class,
                        s -> {
                            log.info("Cleaning up data");
                            this.uploader.getFileStructureDataExtractor().cleanData();
                            log.info("Data cleaned up");
                        }
                )
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}


