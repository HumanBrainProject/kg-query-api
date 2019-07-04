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
