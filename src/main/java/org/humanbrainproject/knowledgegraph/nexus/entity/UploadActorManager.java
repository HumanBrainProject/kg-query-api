package org.humanbrainproject.knowledgegraph.nexus.entity;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.sysmsg.NoMessage;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import org.humanbrainproject.knowledgegraph.nexus.entity.actormsg.*;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;


public class UploadActorManager extends AbstractActor {

    private Long evictionTime = 30 * 60 * 1000L;

    private class CachedActor{
        private ActorRef actorRef;
        private Long time;

        public CachedActor(ActorRef actorRef, Long time ){
            this.actorRef = actorRef;
            this.time = time;
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
    private final Map<UUID, CachedActor> runningActors = new HashMap<>();

    public static Props props() {
        return Props.create(UploadActorManager.class, () -> new UploadActorManager());
    }

    public void createUploadingActor(CreateUploadActorMsg m){
        ActorRef uploadActor = getContext().actorOf(UploadActor.props(m.getUploader()), m.getUuid().toString() + "uploader");
        uploadActor.tell(new UploadDataMsg(m.getUuid()), self());
        this.runningActors.put( m.getUuid(), new CachedActor(uploadActor, null));
    }

    public UploadStatus getStatus(UUID uuid) throws Exception {
        Timeout timeout = Timeout.create(Duration.ofSeconds(5));
        ActorRef actorRef = this.runningActors.get(uuid).getActorRef();
        Future<Object> f = Patterns.ask(actorRef, new GetStatusMsg(uuid), timeout);
        return (UploadStatus) Await.result(f, timeout.duration());

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
                                sender().tell(this.getStatus(s.getUuid()), self());
                            }else{
                                sender().tell(new Object(), self());
                            }
                        }
                )
                .match(
                        CacheEviction.class,
                        s -> {
                           log.info("Cache eviction taking place");
                           Stream<Map.Entry<UUID, CachedActor>> l = this.runningActors.entrySet().stream().filter(entry -> entry.getValue().getTime() != null && entry.getValue().getTime() + evictionTime < System.currentTimeMillis() );
                            l.forEach(el -> {
                                ActorRef ref = el.getValue().getActorRef();
                                ref.tell(new CleanDataMsg(), self());
                                ref.tell(PoisonPill.getInstance(), ActorRef.noSender());
                            });
                           this.runningActors.entrySet().removeIf(entry -> entry.getValue().getTime() != null && entry.getValue().getTime() + evictionTime < System.currentTimeMillis() );
                        }
                )
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }


}
