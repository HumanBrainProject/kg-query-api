package org.humanbrainproject.knowledgegraph.nexus.entity;

import com.typesafe.config.ConfigException;
import org.apache.solr.client.solrj.SolrServerException;
import org.glassfish.jersey.internal.guava.Predicates;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.Query;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.springframework.boot.configurationprocessor.json.JSONException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.ws.rs.BadRequestException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FileStructureDataExtractor {

    private FileStructureData data;
    private ArangoQuery arangoQuery;
    private Map<NexusSchemaReference, Set<File>> filesToHandle = new HashMap();
    private NexusDataStructure nexusDataStructure = new NexusDataStructure();

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


    public FileStructureDataExtractor(FileStructureData data, ArangoQuery arangoQuery){
        this.data = data;
        this.arangoQuery = arangoQuery;
    }

    public NexusDataStructure extractFile() throws IOException, SolrServerException, JSONException {
        File[] files = this.data.listFiles();
        for (File file : files) {
            handleOrgDirectory(file);
        }
        // Creating map id
        this.fetchingCurrentIdentifiers();

        return this.nexusDataStructure;

    }

    // TODO Optimize all filters can be done in one pass par schema
    protected void fetchingCurrentIdentifiers() throws JSONException, SolrServerException, IOException {
        for(NexusSchemaReference r: this.nexusDataStructure.getSchemasConcerned()){
            QueryResult<List<Map>> result = this.arangoQuery.queryPropertyGraphBySpecification(this.query(r));
            Map<String, String> elements = new HashMap<>();
            result.getResults().stream().forEach(i -> elements.put( (String) i.get("identifier"), (String) i.get("uuid")));
            elements.keySet().stream().filter(Predicates.in(
                    new HashSet<>(this.filesToHandle.get(r).stream().map(file -> file.getName().replace(".json", ""))
                            .collect(Collectors.toList()))).negate()).map(elements::get).forEach(e -> this.nexusDataStructure.addToDelete(e));
            this.nexusDataStructure.addToCreate(r, this.filesToHandle.get(r).stream().filter( f -> !elements.containsKey(f.getName().replace(".json", ""))).collect(Collectors.toList()));
            this.filesToHandle.get(r).stream().filter( f -> elements.containsKey(f.getName().replace(".json", ""))).forEach(e -> this.nexusDataStructure.addToUpdate(elements.get(e.getName().replace(".json", "") ), e));
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
                        this.nexusDataStructure.addToSchemasConcerned(ref);
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
        this.filesToHandle.put(ref, s);
    }

    public void cleanData() throws IOException {
        this.data.cleanData();
    }
}
