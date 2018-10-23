package org.humanbrainproject.knowledgegraph.testFactory;

import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.jsonld.control.JsonTransformer;

import java.util.Map;

public class TestObjectFactory {

    public static NexusInstanceReference fooInstanceReference(){
        return new NexusInstanceReference("foo", "bar", "foobar", "v0.0.1", "barfoo");
    }


    public static QualifiedIndexingMessage createQualifiedIndexingMessage(NexusInstanceReference reference, Map<String, Object> qualifiedMap) {
        JsonTransformer jsonTransformer = new JsonTransformer();
        IndexingMessage indexingMessage = new IndexingMessage(reference, jsonTransformer.getMapAsJson(qualifiedMap));
        return new QualifiedIndexingMessage(indexingMessage, qualifiedMap);

    }

}
