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
