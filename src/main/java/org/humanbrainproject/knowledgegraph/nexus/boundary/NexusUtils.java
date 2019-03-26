package org.humanbrainproject.knowledgegraph.nexus.boundary;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusBatchUploader;
import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureData;
import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureDataExtractor;
import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureUploader;
import org.humanbrainproject.knowledgegraph.nexus.entity.UploadStatus;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.zip.ZipInputStream;

@Component
public class NexusUtils {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    NexusBatchUploader uploader;

    @Autowired
    ArangoQuery query;

    @Autowired
    NexusClient client;

    @Autowired
    SchemaController schemaController;


    public UUID uploadFileStructure(InputStream payload, boolean noDeletion) throws IOException, JSONException, SolrServerException {
        BufferedInputStream bis = new BufferedInputStream(payload);
        FileStructureData fs = new FileStructureData(new ZipInputStream(bis));
        FileStructureDataExtractor fe = new FileStructureDataExtractor(fs, query);
        FileStructureUploader fu = new FileStructureUploader(fe.extractFile(), schemaController, client, authorizationContext.getCredential(), fe, noDeletion);
        uploader.uploadFileStructure(fs.getGeneratedId(),fu);
        return fs.getGeneratedId();
    }

    public UploadStatus retreiveUploadStatus(UUID uuid) throws Exception {
        return uploader.retrieveStatus(uuid);
    }
}
