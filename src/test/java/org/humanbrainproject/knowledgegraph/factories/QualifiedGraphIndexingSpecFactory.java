package org.humanbrainproject.knowledgegraph.factories;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.indexing.GraphSpecificationController;
import org.humanbrainproject.knowledgegraph.control.indexing.TestGraphSpecificationController;
import org.humanbrainproject.knowledgegraph.control.json.TestJsonTransformer;
import org.humanbrainproject.knowledgegraph.control.jsonld.TestJsonLdStandardization;
import org.humanbrainproject.knowledgegraph.entity.indexing.GraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.indexing.QualifiedGraphIndexingSpec;

public class QualifiedGraphIndexingSpecFactory {

    private static final String DUMMY_PAYLOAD = "{\"https://schema.hbp.eu/test/foo\": \"bar\"}";

    public static QualifiedGraphIndexingSpec createQualifiedGraphIndexingSpecWithDummyPayload(String entityName, String id){
        return createQualifiedGraphIndexingSpec(entityName, id, DUMMY_PAYLOAD);
    }

    public static QualifiedGraphIndexingSpec createQualifiedGraphIndexingSpec(String entityName, String id, String payload){
        GraphIndexingSpec spec = new GraphIndexingSpec().setEntityName(entityName).setId(id).setJsonOrJsonLdPayload(payload);
        return createQualifiedGraphIndexingSpec(spec);
    }

    public static QualifiedGraphIndexingSpec createQualifiedGraphIndexingSpec(GraphIndexingSpec spec){
        return createGraphSpecificationController().qualify(spec, false);
    }

    private static GraphSpecificationController createGraphSpecificationController(){
        TestGraphSpecificationController specController = new TestGraphSpecificationController();
        TestJsonTransformer testJsonTransformer = new TestJsonTransformer();
        testJsonTransformer.setNamingConvention(new ArangoNamingConvention());
        TestJsonLdStandardization jsonLdStandardization = new TestJsonLdStandardization();
        jsonLdStandardization.setJsonTransformer(testJsonTransformer);
        specController.setJsonLdStandardization(jsonLdStandardization);
        return specController;
    }




}
