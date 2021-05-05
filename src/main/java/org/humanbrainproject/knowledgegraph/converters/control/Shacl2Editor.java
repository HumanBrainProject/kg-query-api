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
