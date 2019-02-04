package org.humanbrainproject.knowledgegraph.converters.control;

import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.converters.entity.*;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Shacl2Editor {

    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    public JsonDocument convert(NexusSchemaReference schemaReference, List<JsonDocument> shaclDocuments) {
        List<ShaclShape> shapes = shaclDocuments.stream().map(shacl -> new ShaclSchema(shacl).getShaclShapes()).flatMap(List::stream).collect(Collectors.toList());
        EditorSpec editorSpec = convertShapeToSpec(schemaReference, shapes);
        JsonDocument result = new JsonDocument();
        result.addToProperty("uiSpec", new JsonDocument().addToProperty(schemaReference.getOrganization(), new JsonDocument().addToProperty(schemaReference.getDomain(), new JsonDocument().addToProperty(schemaReference.getSchema(), new JsonDocument().addToProperty(schemaReference.getSchemaVersion(), editorSpec.toJson())))));
        return result;
    }

    private EditorSpec convertShapeToSpec(NexusSchemaReference schemaReference, List<ShaclShape> shaclShape) {
        List<EditorSpecField> fields = new ArrayList<>();
        Set<String> existingKeys = new HashSet<>();
        for (ShaclShape shape : shaclShape) {
            if (shape.isTargeted()) {
                collectFields(fields, existingKeys, shape, shaclShape);
            }
        }
        return new EditorSpec(schemaReference, shaclShape.get(0).getLabel(), null, null, fields);
    }

    private void collectFields(List<EditorSpecField> fields, Set<String> existingKeys, ShaclShape currentShape, List<ShaclShape> allShapes) {
        fields.addAll(currentShape.getProperties().stream().filter(p -> p.getKey() != null && !existingKeys.contains(p.getKey())).map(p -> {
            existingKeys.add(p.getKey());
            String name = p.getName() == null ? semanticsToHumanTranslator.translateSemanticValueToHumanReadableLabel(p.getKey()) : p.getName();
            EditorSpecField editorSpecField = new EditorSpecField(p.getKey(), name, p.getShapeDeclaration());
            if (p.isLinkToInstance()) {
                editorSpecField.setLinkToOtherInstance(true);
            }
            return editorSpecField;
        }).collect(Collectors.toList()));
        List<String> nodes = currentShape.getNodes();
        for (String node : nodes) {
            for (ShaclShape dependentShape : allShapes) {
                if (dependentShape.getId().equals(node)) {
                    collectFields(fields, existingKeys, dependentShape, allShapes);
                }
            }
        }
    }


}
