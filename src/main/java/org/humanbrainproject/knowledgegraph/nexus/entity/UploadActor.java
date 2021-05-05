/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

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


