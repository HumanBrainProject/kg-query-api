package org.humanbrainproject.knowledgegraph.nexus.control;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureUploader;
import org.humanbrainproject.knowledgegraph.nexus.entity.UploadActorManager;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.CacheEviction;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.CreateUploadActorMsg;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.GetStatusMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@EnableScheduling
public class NexusBatchUploader {

    @Autowired
    private ActorSystem actorSystem;

    private ActorRef actorManager = null;

    private ActorRef getActorManager(){
        if(actorManager == null){
            actorManager = actorSystem.actorOf(UploadActorManager.props(), "UploadActorManager");
        }
        return actorManager;
    }

    public void uploadFileStructure(UUID uuid, FileStructureUploader uploader) {
        getActorManager().tell(new CreateUploadActorMsg(uuid, uploader), ActorRef.noSender());
    }

    public CompletableFuture<Object> retrieveStatus(UUID uuid) {
        Duration timeout = Duration.ofSeconds(25);
        CompletableFuture<Object> future = Patterns.ask(getActorManager(), new GetStatusMsg(uuid), timeout).toCompletableFuture();
        return future;
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 5000)
    public void evictCache(){
        getActorManager().tell(new CacheEviction(), ActorRef.noSender());
    }

}
