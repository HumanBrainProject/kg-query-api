package org.humanbrainproject.knowledgegraph.nexus.entity;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.CleanDataMsg;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.GetStatusMsg;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.UploadDataMsg;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.UploadDoneMsg;

public class UploadActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final FileStructureUploader uploader;

    static Props props(FileStructureUploader uploader) {
        return Props.create(UploadActor.class, () -> new UploadActor(uploader));
    }

    public UploadActor(FileStructureUploader uploader){
        this.uploader = uploader;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        UploadDataMsg.class,
                        s -> {
                            log.info("Upload data message received");
                            this.uploader.uploadData();
                            sender().tell(new UploadDoneMsg(s.getId()), self());
                            log.info("Upload data done!");
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


