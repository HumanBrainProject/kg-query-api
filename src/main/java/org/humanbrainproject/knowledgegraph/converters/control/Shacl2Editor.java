package org.humanbrainproject.knowledgegraph.converters.control;

import org.humanbrainproject.knowledgegraph.converters.entity.EditorSpec;
import org.humanbrainproject.knowledgegraph.converters.entity.EditorSpecField;
import org.humanbrainproject.knowledgegraph.converters.entity.ShaclSchema;
import org.humanbrainproject.knowledgegraph.converters.entity.ShaclShape;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Shacl2Editor {

    public JsonDocument convert(NexusSchemaReference schemaReference, JsonDocument shacl){

        List<EditorSpec> editorSpecs = new ShaclSchema(shacl).getShaclShapes().stream().map(s -> convertShapeToSpec(schemaReference, s)).collect(Collectors.toList());
        JsonDocument result = new JsonDocument();
        List<JsonDocument> instances = editorSpecs.stream().map(e -> e.toJson()).collect(Collectors.toList());
        result.addToProperty("uiSpec", new JsonDocument().addToProperty(schemaReference.getOrganization(), new JsonDocument().addToProperty(schemaReference.getDomain(), new JsonDocument().addToProperty(schemaReference.getSchema(), new JsonDocument().addToProperty(schemaReference.getSchemaVersion(), instances)))));
        return result;
    }

    private EditorSpec convertShapeToSpec(NexusSchemaReference schemaReference, ShaclShape shaclShape){

        List<EditorSpecField> fields = shaclShape.getProperties().stream().map(p -> {
           EditorSpecField editorSpecField = new EditorSpecField(p.getKey(), p.getName());
           if(p.isLinkToInstance()){
               editorSpecField.setLinkToOtherInstance(true);
           }
           return editorSpecField;
        }).collect(Collectors.toList());

        return new EditorSpec(schemaReference, shaclShape.getLabel(), null, null, fields);
    }



}
