package org.humanbrainproject.knowledgegraph.converters.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.converters.control.Shacl2Editor;
import org.humanbrainproject.knowledgegraph.converters.control.ShaclResolver;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Converter {

    @Autowired
    ShaclResolver resolver;

    @Autowired
    Shacl2Editor shacl2Editor;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    AuthorizationContext authorizationContext;

    public JsonDocument convertShaclToEditor(NexusSchemaReference schemaReference){
        List<JsonDocument> resolvedAndQualified = resolver.resolve(schemaReference);
        return shacl2Editor.convert(schemaReference, resolvedAndQualified);
    }


}
