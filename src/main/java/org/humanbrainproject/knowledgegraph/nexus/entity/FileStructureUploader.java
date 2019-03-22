package org.humanbrainproject.knowledgegraph.nexus.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrServerException;
import org.glassfish.jersey.internal.guava.Predicates;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Query;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.springframework.boot.configurationprocessor.json.JSONException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.ws.rs.BadRequestException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FileStructureUploader {

    private FileStructureData data;
    private NexusClient nexusClient;
    private SchemaController schemaController;
    private ArangoQuery arangoQuery;
    private AuthorizationContext authorizationContext;

    private List<String> toDelete = new ArrayList<>();
    private Map<NexusSchemaReference, List<File>> toCreate = new HashMap<>();
    private Map<String, File> toUpdate = new HashMap<>();

    private Set<NexusSchemaReference> schemasConcerned = new HashSet(); // Redundant?
    private Map<NexusSchemaReference, Set<File>> filesToHandle = new HashMap();

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
                "    }" +
                "  ]" +
                "}", ref, "https://schema.hbp.eu/myQuery/");
    }



    // TODO Add retry mechanism
    public FileStructureUploader(FileStructureData data, NexusClient nexusClient, SchemaController schemaController,
                                 ArangoQuery arangoQuery, AuthorizationContext authorizationContext){
        this.data = data;
        this.nexusClient = nexusClient;
        this.schemaController = schemaController;
        this.arangoQuery = arangoQuery;
        this.authorizationContext = authorizationContext;
    }

    public void uploadData() throws IOException, JSONException, SolrServerException {
        File[] files = this.data.listFiles();
        for (File file : files) {
            handleOrgDirectory(file);
        }
        // Creating map id
        this.fetchingCurrentIdentifiers();
        for(NexusSchemaReference s: this.schemasConcerned){
            this.schemaController.createSchema(s);
        }

        if(!data.isNoDeletion()){
            for(String s: this.toDelete){
                NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, s);
                JsonDocument doc = this.nexusClient.get(url, this.authorizationContext.getCredential());
                Integer rev = doc.getNexusRevision();
                this.nexusClient.delete(url, rev,this.authorizationContext.getCredential());
            }
        }

        for(Map.Entry<String, File> el: this.toUpdate.entrySet()){
            NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, el.getKey());
            JsonDocument doc = this.nexusClient.get(url, this.authorizationContext.getCredential());
            Integer rev = doc.getNexusRevision();
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> json = mapper.readValue(el.getValue(), Map.class);
            this.nexusClient.put(url, rev, json, this.authorizationContext.getCredential());
        }

        for(Map.Entry<NexusSchemaReference, List<File>> el: this.toCreate.entrySet()){
            for(File f: el.getValue()){
                NexusRelativeUrl url = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, el.getKey().toString());
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> json = mapper.readValue(f, Map.class);
                this.nexusClient.post(url, null, json, this.authorizationContext.getCredential());
            }
        }
    }

    // TODO Optimize all filters can be done in one pass par schema
    protected void fetchingCurrentIdentifiers() throws JSONException, SolrServerException, IOException {
        for(NexusSchemaReference r: this.schemasConcerned){
            QueryResult<List<Map>> result = this.arangoQuery.queryPropertyGraphBySpecification(this.query(r));
            Map<String, String> elements = new HashMap<>();
            result.getResults().stream().forEach(i -> elements.put( (String) i.get("identifier"), (String) i.get("uuid")));
            this.toDelete.addAll(elements.keySet().stream().filter(Predicates.in(
                    new HashSet<>(this.filesToHandle.get(r).stream().map(file -> file.getName().replace(".json", ""))
                            .collect(Collectors.toList()))).negate()).map(elements::get).collect(Collectors.toList()));
            this.toCreate.put(r, this.filesToHandle.get(r).stream().filter( f -> !elements.containsKey(f.getName().replace(".json", ""))).collect(Collectors.toList()));
            this.filesToHandle.get(r).stream().filter( f -> elements.containsKey(f.getName().replace(".json", ""))).forEach(e -> this.toUpdate.put(elements.get(e.getName().replace(".json", "") ), e));
        }
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
                        schemasConcerned.add(ref);
                        File schemaJsonFile = Arrays.stream(schemaVersionOrFile.listFiles()).filter(f -> f.getName().equals("schema.json")).findAny().orElse(null);
                        handleSchemaUpload(schemaJsonFile, ref);
                        for (File jsonFile : schemaVersionOrFile.listFiles()) {
                            if(!jsonFile.getName().equals("schema.json")){
                                handleJsonFile(jsonFile, ref);
                            }
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
    protected void handleSchemaUpload(File file, NexusSchemaReference ref){

    }

    protected void handleJsonFile(File file, NexusSchemaReference ref){
        Set<File> s = this.filesToHandle.getOrDefault(ref, new HashSet());
        s.add(file);
        filesToHandle.put(ref, s);
    }
}
