package org.humanbrainproject.knowledgegraph.converters.boundary;

import org.humanbrainproject.knowledgegraph.converters.control.Shacl2Editor;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Converter {

    @Autowired
    Shacl2Editor shacl2Editor;

    public JsonDocument convertShaclToEditor(NexusSchemaReference schemaReference){
        //TODO load shacl schema from KG
        JsonDocument shaclPayload = new JsonDocument();
        return shacl2Editor.convert(schemaReference, shaclPayload);
    }

}
