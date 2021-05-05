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

package org.humanbrainproject.knowledgegraph.nexus.boundary;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.instances.control.ContextController;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusBatchUploader;
import org.humanbrainproject.knowledgegraph.nexus.entity.*;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.DatabaseScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import javax.ws.rs.NotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipInputStream;

@Component
public class NexusUtils {

    @Autowired
    NexusBatchUploader uploader;

    @Autowired
    NexusClient client;

    @Autowired
    SchemaController schemaController;

    @Autowired
    ContextController contextController;

    @Autowired
    ArangoQuery query;

    @Autowired
    JsonLdStandardization standardization;

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;

    public UUID uploadFileStructure(InputStream payload, boolean noDeletion, boolean isSimulation) throws IOException, SolrServerException, JSONException, NoSuchAlgorithmException {
        BufferedInputStream bis = new BufferedInputStream(payload);
        FileStructureData fs = new FileStructureData(new ZipInputStream(bis));
        fs.listFiles();
        FileStructureDataExtractor fe = new FileStructureDataExtractor(fs);
        //We need to ensure that we're operating on the native space. -> let's switch the database scope.
        queryContext.setDatabaseScope(DatabaseScope.NATIVE);
        NexusDataStructure data = fe.extractFile(query, standardization);
        FileStructureUploader fu = new FileStructureUploader( data, schemaController, contextController, client,authorizationContext.getCredential(), fe, noDeletion, isSimulation);
        uploader.uploadFileStructure(fs.getGeneratedId(),fu);
        return fs.getGeneratedId();
    }

    public UploadStatus retreiveUploadStatus(UUID uuid) throws NotFoundException, ExecutionException, InterruptedException {
        Object o = uploader.retrieveStatus(uuid).get();
        if(o instanceof UploadStatus){
            return (UploadStatus)o;
        }else{
            throw new NotFoundException("Status not found");
        }
    }
}
