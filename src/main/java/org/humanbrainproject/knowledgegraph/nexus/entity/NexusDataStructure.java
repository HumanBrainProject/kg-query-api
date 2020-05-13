/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.nexus.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

import java.io.File;
import java.util.*;

public class NexusDataStructure {

    private List<String> toDelete = new ArrayList<>();
    private Map<NexusSchemaReference, List<File>> toCreate = new HashMap<>();
    private Map<String, File> toUpdate = new HashMap<>();
    private Map<NexusSchemaReference, File> schemasConcerned = new HashMap<>();
    private Map<NexusSchemaReference, File> contextFiles = new HashMap<>();

    public NexusDataStructure(){ }

    public List<String> getToDelete() {
        return toDelete;
    }

    public Map<NexusSchemaReference, File> getContextFiles(){
        return this.contextFiles;
    }

    public void addToDelete(String toDelete) {
        this.toDelete.add(toDelete);
    }

    public Map<NexusSchemaReference, List<File>> getToCreate() {
        return toCreate;
    }

    public void addToCreate(NexusSchemaReference ref, File toCreate) {
        List<File> r = this.toCreate.getOrDefault(ref, new ArrayList<File>());
        if(r == null){
            this.toCreate.put(ref, Collections.singletonList(toCreate));
        }else{
            r.add(toCreate);
            this.toCreate.put(ref, r);
        }
    }

    public Map<String, File> getToUpdate() {
        return toUpdate;
    }

    public void addToUpdate(String key, File toUpdate) {
        this.toUpdate.put(key, toUpdate);
    }

    public Map<NexusSchemaReference, File> getSchemasConcerned() {
        return schemasConcerned;
    }

    public void addToSchemasConcerned(NexusSchemaReference schemasConcerned, File schemaFile) {
        this.schemasConcerned.put(schemasConcerned, schemaFile);
    }

    public void addToContext(NexusSchemaReference ref, File context){
        this.contextFiles.put(ref, context);
    }
}
