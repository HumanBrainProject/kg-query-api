package org.humanbrainproject.knowledgegraph.nexus.entity;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.*;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class UploadActorManager extends AbstractActor {

    private Long evictionTime = 30 * 60 * 1000L;

    private class CachedActor{
        private ActorRef actorRef;
        private Long time;
        private boolean toEvict;

        public CachedActor(ActorRef actorRef, Long time, boolean toEvict){
            this.actorRef = actorRef;
            this.time = time;
            this.toEvict = toEvict;
        }

        public void setTime(Long time) {
            this.time = time;
        }

        public Long getTime() {
            return time;
        }

        public ActorRef getActorRef() {
            return actorRef;
        }
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Map<UUID, CachedActor> runningActors = new ConcurrentHashMap<>();

    public static Props props() {
        return Props.create(UploadActorManager.class, () -> new UploadActorManager());
    }

    public void createUploadingActor(CreateUploadActorMsg m){
        ActorRef uploadActor = getContext().actorOf(UploadActor.props(m.getUploader(), self()), m.getUuid().toString());
        this.runningActors.put( m.getUuid(), new CachedActor(uploadActor, -1L, m.getUploader().getStatus().isSimulation()));
        uploadActor.tell(new UploadDataMsg(m.getUuid()), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        CreateUploadActorMsg.class,
                        s -> {
                            log.info("Upload data message received");
                            this.createUploadingActor(s);
                        })
                .match(
                        UploadDoneMsg.class,
                        s -> {
                            log.info("Actor with uuid " + s.getId() + " is done uploading data");
                            CachedActor c = this.runningActors.get(s.getId());
                            c.setTime(System.currentTimeMillis());
                            this.runningActors.put(s.getId(), c);
                        }
                )
                .match(
                        GetStatusMsg.class,
                        s -> {
                            if(this.runningActors.containsKey(s.getUuid())) {
                                Duration timeout =Duration.ofSeconds(25);
                                ActorRef actorRef = this.runningActors.get(s.getUuid()).getActorRef();
                                CompletableFuture<Object> fut = Patterns.ask(actorRef,new GetStatusMsg(s.getUuid()), timeout).toCompletableFuture();
                                Patterns.pipe(fut, getContext().getDispatcher()).to(getSender());
                            }else{
                                sender().tell(new Object(), self());
                            }
                        }
                )
                .match(
                        CacheEviction.class,
                        s -> {
                           log.info("Cache eviction taking place");
                            Set<Map.Entry<UUID, CachedActor>> toBeRemoved = this.runningActors.entrySet().stream().filter(entry -> entry.getValue().toEvict || (entry.getValue().getTime() != -1L && entry.getValue().getTime() + evictionTime < System.currentTimeMillis())).collect(Collectors.toSet());
                            toBeRemoved.forEach(el -> {
                                ActorRef ref = el.getValue().getActorRef();
                                ref.tell(new CleanDataMsg(), self());
                                ref.tell(PoisonPill.getInstance(), ActorRef.noSender());
                            });
                           this.runningActors.entrySet().removeIf(toBeRemoved::contains);
                        }
                )
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }


}
