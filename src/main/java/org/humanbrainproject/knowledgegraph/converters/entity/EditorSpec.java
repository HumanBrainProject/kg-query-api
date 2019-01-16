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
