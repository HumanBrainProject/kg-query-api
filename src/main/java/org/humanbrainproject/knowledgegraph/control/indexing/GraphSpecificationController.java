package org.humanbrainproject.knowledgegraph.control.indexing;

import org.humanbrainproject.knowledgegraph.control.json.JsonTransformer;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.entity.indexing.GraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.indexing.QualifiedGraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GraphSpecificationController {

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges;

    @Autowired
    JsonTransformer jsonTransformer;

    public QualifiedGraphIndexingSpec qualify(GraphIndexingSpec spec, Map map) {
        Map qualifiedMap = postProcessMap(map, spec.getDefaultNamespace());
        List<JsonLdVertex> jsonLdVertices = jsonLdToVerticesAndEdges.transformFullyQualifiedJsonLdToVerticesAndEdges(spec, qualifiedMap);
        return new QualifiedGraphIndexingSpec(spec, qualifiedMap, jsonLdVertices);
    }

    public QualifiedGraphIndexingSpec qualify(GraphIndexingSpec spec) {
        return qualify(spec, jsonTransformer.parseToMap(spec.getJsonOrJsonLdPayload()));
    }

    private Map postProcessMap(Map data, String defaultNamespace) {
        Map result = jsonLdStandardization.ensureContext(data, defaultNamespace);
        result = jsonLdStandardization.fullyQualify(result);
        result = jsonLdStandardization.filterKeysByVocabBlacklists(result);
        return result;
    }

}
