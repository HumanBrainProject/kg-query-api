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

package org.humanbrainproject.knowledgegraph.converters.entity;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.List;
import java.util.stream.Collectors;

public class EditorSpec {

    private final NexusSchemaReference schemaReference;
    private final String label;
    private final String folderId;
    private final String folderName;
    private final List<EditorSpecField> fields;

    public EditorSpec(NexusSchemaReference schemaReference, String label, String folderId, String folderName, List<EditorSpecField> fields){
        this.schemaReference = schemaReference;
        this.label = label;
        this.folderId = folderId;
        this.folderName = folderName;
        this.fields = fields;
    }

    public JsonDocument toJson(){
        JsonDocument instance  = new JsonDocument();
        //fill instance
        instance.addToProperty("label", label);
        if(folderId!=null) {
            instance.addToProperty("folderID", folderId);
        }
        if(folderName!=null){
            instance.addToProperty("folderName", folderName);
        }
        if(fields!=null) {
            instance.addToProperty("fields", fields.stream().map(f -> f.toJson()).collect(Collectors.toList()));
        }
        return instance;
    }


}
