package org.humanbrainproject.knowledgegraph.converters.control;

import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.converters.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Shacl2Editor {

    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    public JsonDocument convert(NexusSchemaReference schemaReference, List<JsonDocument> shaclDocuments){
        List<ShaclShape> shapes = shaclDocuments.stream().map(shacl -> new ShaclSchema(shacl).getShaclShapes()).flatMap(List::stream).collect(Collectors.toList());
        EditorSpec editorSpec = convertShapeToSpec(schemaReference, shapes);
        if(editorSpec!=null) {
            JsonDocument result = new JsonDocument();
            result.addToProperty("uiSpec", new JsonDocument().addToProperty(schemaReference.getOrganization(), new JsonDocument().addToProperty(schemaReference.getDomain(), new JsonDocument().addToProperty(schemaReference.getSchema(), new JsonDocument().addToProperty(schemaReference.getSchemaVersion(), editorSpec.toJson())))));
            return result;
        }
        return null;
    }

    private EditorSpec convertShapeToSpec(NexusSchemaReference schemaReference, List<ShaclShape> shaclShape){
        List<ShaclProperty> properties = shaclShape.stream().map(shape -> shape.getProperties()).flatMap(List::stream).collect(Collectors.toList());
        List<EditorSpecField> fields = properties.stream().map(p -> {
            String name = p.getName()==null ? semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel(p.getKey()) : p.getName();
            EditorSpecField editorSpecField = new EditorSpecField(p.getKey(), name, p.getShapeDeclaration());
           if(p.isLinkToInstance()){
               editorSpecField.setLinkToOtherInstance(true);
           }
           return editorSpecField;
        }).collect(Collectors.toList());
        if(!shaclShape.isEmpty()) {
            return new EditorSpec(schemaReference, shaclShape.get(0).getLabel(), null, null, fields);
        }
        return null;
    }



}
