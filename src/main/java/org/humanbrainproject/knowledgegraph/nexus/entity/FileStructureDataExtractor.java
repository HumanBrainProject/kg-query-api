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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.lang.NotImplementedException;
import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Tuple;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.Query;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;

import javax.ws.rs.BadRequestException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class FileStructureDataExtractor {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(FileStructureUploader.class);
    private FileStructureData data;
    private Map<NexusSchemaReference, Set<File>> filesToHandle = new HashMap();
    private NexusDataStructure nexusDataStructure = new NexusDataStructure();
    private ObjectMapper mapper = new ObjectMapper();
    private Query query(NexusSchemaReference ref){
        return new Query("{" +
                "  \"@context\": {" +
                "    \"@vocab\": \"https://schema.hbp.eu/graphQuery/\"," +
                "    \"query\": \"https://schema.hbp.eu/myQuery/\"," +
                "    \"fieldname\": {" +
                "      \"@id\": \"fieldname\"," +
                "      \"@type\": \"@id\"" +
                "    }," +
                "    \"relative_path\": {" +
                "      \"@id\": \"relative_path\"," +
                "      \"@type\": \"@id\"" +
                "    }" +
                "  }," +
                "  \"fields\": [" +
                "    {" +
                "      \"fieldname\": \"query:identifier\"," +
                "      \"relative_path\": {" +
                "        \"@id\": \"http://schema.org/identifier\"" +
                "      }," +
                "      \"required\":true" +
                "    }," +
                "    {" +
                "      \"fieldname\": \"query:uuid\"," +
                "      \"relative_path\": {" +
                "        \"@id\": \"https://schema.hbp.eu/relativeUrl\"" +
                "      }," +
                "      \"required\":true" +
                "    }," +
                "    {" +
                "      \"fieldname\": \"query:hashcode\"," +
                "      \"relative_path\": {" +
                "        \"@id\": \"https://schema.hbp.eu/internal/hashcode\"" +
                "      }" +
                "    }" +
                "  ]" +
                "}", ref, "https://schema.hbp.eu/myQuery/");
    }

    public FileStructureDataExtractor(FileStructureData data){
        this.data = data;
    }

    public NexusDataStructure extractFile(ArangoQuery query, JsonLdStandardization standardization) throws IOException, SolrServerException, JSONException, NoSuchAlgorithmException {
        File[] files = this.data.listFiles();
        for (File file : files) {
            handleOrgDirectory(file);
        }
        // Creating map id
        this.fetchingCurrentIdentifiers(query, standardization);
        return this.nexusDataStructure;

    }

    protected void fetchingCurrentIdentifiers(ArangoQuery query, JsonLdStandardization standardization) throws IOException, SolrServerException, JSONException, NoSuchAlgorithmException {
        for(Map.Entry<NexusSchemaReference, File> r: this.nexusDataStructure.getSchemasConcerned().entrySet()){
            QueryResult<List<Map>> result = query.queryPropertyGraphBySpecification(this.query(r.getKey()), null);
            Map<String, Tuple<String, String>> idToUUIDMap = new HashMap<>();
            result.getResults().stream().forEach(i -> {
                List<String> identifiers;
                if(i.get("identifier") instanceof List){
                    identifiers = (List<String>) i.get("identifier");
                }else{
                    identifiers = Collections.singletonList((String) i.get("identifier"));
                }
                identifiers.forEach(el -> idToUUIDMap.put( el, new Tuple((String) i.get("uuid"), (String) i.get("hashcode"))) );

            });

            List<String> identifierLocal = fillCreateAndUpdateList(this.filesToHandle, idToUUIDMap, r.getKey(), standardization);
            idToUUIDMap.entrySet().stream().forEach( entry -> {
                if(!identifierLocal.contains(entry.getKey())){
                    this.nexusDataStructure.addToDelete(entry.getValue().getValue1());
                }
            });
        }
    }

    protected List<String> fillCreateAndUpdateList( Map<NexusSchemaReference, Set<File>> filesToHandle, Map<String, Tuple<String, String>> idToUUIDMap,
                                                    NexusSchemaReference ref, JsonLdStandardization standardization) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("MD5");
        List<String> identifierLocal = new ArrayList<>();
        filesToHandle.getOrDefault(ref, new HashSet<File>()).stream().forEach( file -> {
            try{
                Map<String, Object> json = mapper.readValue(file, Map.class);
                Map<String, Object> fullyQualifiedJson = standardization.fullyQualify(json);

                Object identifier = fullyQualifiedJson.get(SchemaOrgVocabulary.IDENTIFIER);
                List<String> identifierList;
                if(identifier instanceof List){
                    identifierList = (List<String>) identifier;
                }else{
                    identifierList = Collections.singletonList((String) identifier);
                }
                identifierLocal.addAll(identifierList);
                String uuid = null;
                String hashcode = null;
                for(String id: identifierList){
                    if(idToUUIDMap.get(id) != null){
                        Tuple<String, String> t = idToUUIDMap.get(id);
                        uuid = t.getValue1();
                        hashcode = t.getValue2();
                        break;
                    }
                }
                String fileContent = fullyQualifiedJson.toString();
                md.update(fileContent.getBytes());
                // bytes to hex
                StringBuilder checksum = new StringBuilder();
                for (byte b : md.digest()) {
                    checksum.append(String.format("%02x", b));
                }
                Gson gson = new Gson();
                json.put(HBPVocabulary.INTERNAL_HASHCODE, checksum);
                FileWriter fileWriter = null;
                String content = gson.toJson(json);
                try {
                    fileWriter = new FileWriter(file);
                    fileWriter.write(content);
                } catch (Exception e){
                    e.printStackTrace();
                }finally {
                    try {
                        if (fileWriter != null) {
                            fileWriter.flush();
                            fileWriter.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(uuid != null ){
                    if(!checksum.toString().equals(hashcode)){
                        this.nexusDataStructure.addToUpdate(uuid, file);
                    }
                }else{
                    this.nexusDataStructure.addToCreate(ref, file);
                }
            } catch (IOException e){
                e.printStackTrace();
                logger.error("Could not open the file " + e.getMessage());
            }
        });
        return identifierLocal;
    }

    protected void handleOrgDirectory(File file){
        if(file.isDirectory()){
            for(File projectFolder : file.listFiles()){
                handleProjectFolder(projectFolder, file.getName());
            }
        } else {
            throw new BadRequestException("Cannot interprete organization folder structure");
        }
    }

    protected void handleProjectFolder(File file, String org){
        if(file.isDirectory()){
            for(File schemaFolder : file.listFiles()){
                handleSchemaFolder(schemaFolder, org, file.getName());
            }
        } else {
            throw new BadRequestException("Cannot interprete project folder structure");
        }
    }

    protected void handleSchemaFolder(File file, String org, String project){
        if(file.isDirectory()){
            if(file.getName().equals("_")) {
                throw new NotImplementedException();
            }else {
                for (File schemaVersionOrFile : file.listFiles()) {
                    if (schemaVersionOrFile.isDirectory()) {
                        NexusSchemaReference ref = new NexusSchemaReference(org, project,file.getName(), schemaVersionOrFile.getName());
                        this.nexusDataStructure.addToSchemasConcerned(ref, null);
                        for (File jsonFile : schemaVersionOrFile.listFiles()) {
                            handleJsonFile(jsonFile, ref);
                        }
                    } else {
                        throw new NotImplementedException();
                    }
                }
            }
        } else {
            throw new BadRequestException("Cannot interprete schema folder structure");
        }
    }

    protected void handleJsonFile(File file, NexusSchemaReference ref){
        Set<File> s = this.filesToHandle.getOrDefault(ref, new HashSet());
        switch(file.getName()){
            case "schema.json":
                this.nexusDataStructure.addToSchemasConcerned(ref, file);
                break;
            case "context.json":
                this.nexusDataStructure.addToContext(ref, file);
                break;
            default:
                s.add(file);
                this.filesToHandle.put(ref,s);
        }
    }

    public void cleanData() throws IOException {
        this.data.cleanData();
    }
}
