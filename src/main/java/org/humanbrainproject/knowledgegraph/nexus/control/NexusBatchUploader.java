package org.humanbrainproject.knowledgegraph.nexus.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrServerException;
import org.glassfish.jersey.internal.guava.Predicates;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureData;
import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureUploader;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Query;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class NexusBatchUploader {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    ArangoQuery arangoQuery;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    SchemaController schemaController;

    private Logger log = LoggerFactory.getLogger(NexusBatchUploader.class);

    public void uploadFileStructure(FileStructureData data){
        try{
            new FileStructureUploader(data, nexusClient, schemaController, arangoQuery, authorizationContext).uploadData();
        } catch (IOException e){
            log.error(String.format("Could not save zip structure %s", e.getMessage() ));
            throw new InternalServerErrorException("Could not upload file");
        } catch (JSONException e){
            throw new InternalServerErrorException("Could not execute query");
        } catch (SolrServerException e){
            throw new InternalServerErrorException("Could not properly fetch data");
        }


    }



}
