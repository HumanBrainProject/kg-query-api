package org.humanbrainproject.knowledgegraph.testFactory;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.control.MessageProcessor;
import org.humanbrainproject.knowledgegraph.indexing.control.nexusToArango.NexusToArangoIndexingProvider;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics.SpatialAnchoring;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.query.entity.BoundingBox;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestObjectFactory {


    public static NexusConfiguration createNexusConfiguration(){
        NexusConfiguration nexusConfiguration = Mockito.spy(new NexusConfiguration());
        Mockito.doReturn("http://foo").when(nexusConfiguration).getNexusBase();
        Mockito.doReturn("http://bar").when(nexusConfiguration).getNexusEndpoint();
        return nexusConfiguration;

    }

    public static SpecField simpleSpecFieldWithOutgoingTraverses(String name, String... traversePath){
        List<SpecTraverse> traverses = new ArrayList<>();
        for (String s : traversePath) {
            traverses.add(new SpecTraverse(s, false));
        }

        return new SpecField(name, null, traverses,null, false, false, false, false, null, null);
    }


    public static BoundingBox createBoundingBox(){
        return new BoundingBox(0, 0, 0, 10, 10, 10, "refSpace");

    }


    public static Map<String, Object> createInstanceSkeleton(String identifier) {
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put(SchemaOrgVocabulary.IDENTIFIER, identifier);
        return instance;
    }


    public static Map<String, Object> createArangoInstanceSkeleton(String identifier, String permissionGroup){
        Map<String, Object> foobar = createInstanceSkeleton(identifier);
        foobar.put(ArangoVocabulary.PERMISSION_GROUP, permissionGroup);
        return foobar;
    }

    public static NexusInstanceReference fooInstanceReference() {
        return new NexusInstanceReference("foo", "bar", "foobar", "v0.0.1", "barfoo");
    }

    public static NexusInstanceReference fooEditorInstanceReference() {
        return new NexusInstanceReference("fooeditor", "bar", "foobar", "v0.0.1", "editbar");
    }

    public static IndexingMessage createIndexingMessage(NexusInstanceReference reference, Map<String, Object> map) {
        JsonTransformer jsonTransformer = new JsonTransformer();
        return new IndexingMessage(reference, jsonTransformer.getMapAsJson(map), "2018-10-31", "Foo");
    }


    public static QualifiedIndexingMessage createQualifiedIndexingMessage(NexusInstanceReference reference, Map<String, Object> qualifiedMap) {
        return new QualifiedIndexingMessage(createIndexingMessage(reference, qualifiedMap), qualifiedMap);
    }

    private static Map<String, Object> createSpatialAnchoringPayload() {
        Map<String, Object> fullyQualified = new LinkedHashMap<>();
        fullyQualified.put(JsonLdConsts.TYPE, HBPVocabulary.SPATIAL_TYPE);
        fullyQualified.put(HBPVocabulary.SPATIAL_LOCATED_INSTANCE, "https://nexus-dev.humanbrainproject.org/data/test/core/test/v1.0.0/foo");
        fullyQualified.put(HBPVocabulary.SPATIAL_REFERENCESPACE, "bar");
        fullyQualified.put(HBPVocabulary.SPATIAL_FORMAT, SpatialAnchoring.SpatialAnchoringFormat.QuickNii.getIdentifier());
        fullyQualified.put(HBPVocabulary.SPATIAL_COORDINATES, "116.76450275662296, 420.38180695602125, 371.05990195986874, 32.25523977201931, -483.5743708352436, -69.49201572740994, 88.9241921312597, 51.94987159616494, -320.22912581890387");
        return fullyQualified;
    }


    public static IndexingMessage createSpatialAnchoringIndexingMessage() {
        return createIndexingMessage(TestObjectFactory.fooInstanceReference(), createSpatialAnchoringPayload());
    }

    public static QualifiedIndexingMessage createSpatialAnchoringQualifiedIndexingMessage() {
        return createQualifiedIndexingMessage(TestObjectFactory.fooInstanceReference(), createSpatialAnchoringPayload());
    }

    public static MessageProcessor mockedMessageProcessor(){
        return Mockito.mock(MessageProcessor.class);
    }

    public static NexusToArangoIndexingProvider mockedIndexingProvider(){
        return Mockito.mock(NexusToArangoIndexingProvider.class);
    }

    public static Credential credential(){
        return new InternalMasterKey();
    }
}
