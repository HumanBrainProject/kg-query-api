package org.humanbrainproject.knowledgegraph.testFactory;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestObjectFactory {

    public static Map<String, Object> createInstanceSkeleton(String identifier){
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put(SchemaOrgVocabulary.IDENTIFIER, "foobar");
        return instance;
    }

    public static NexusInstanceReference fooInstanceReference(){
        return new NexusInstanceReference("foo", "bar", "foobar", "v0.0.1", "barfoo");
    }

    public static NexusInstanceReference fooEditorInstanceReference(){
        return new NexusInstanceReference("fooeditor", "bar", "foobar", "v0.0.1", "editbar");
    }

    public static IndexingMessage createIndexingMessage(NexusInstanceReference reference, Map<String, Object> map){
        JsonTransformer jsonTransformer = new JsonTransformer();
        return new IndexingMessage(reference, jsonTransformer.getMapAsJson(map), "2018-10-31", "Foo");
    }


    public static QualifiedIndexingMessage createQualifiedIndexingMessage(NexusInstanceReference reference, Map<String, Object> qualifiedMap) {
       return new QualifiedIndexingMessage(createIndexingMessage(reference, qualifiedMap), qualifiedMap);
    }

}
