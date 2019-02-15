package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QueryBuilderNewTest {

    private final static Set<String> COLLECTION_WHITELIST = new HashSet<>(Arrays.asList("minds", "neuralactivity", "foo", "bar", "cscs"));

    private final static String DATASET_ENDPOINT = "http://test/v0/minds/core/dataset/v1.0.0";
    private final static String SPECIES_ENDPOINT = "http://test/v0/minds/core/species/v1.0.0";


    private Specification readSpecification(String fileName, String endpoint) throws IOException, JSONException {
        String specification = IOUtils.toString(this.getClass().getResourceAsStream("/apiSpec/"+fileName), "UTF-8");
        JsonTransformer jsonTransformer = new JsonTransformer();
        String qualifiedSpec = jsonTransformer.getMapAsJson(new JsonLdStandardization().fullyQualify(jsonTransformer.parseToMap(specification)));
        SpecificationInterpreter specificationInterpreter = new SpecificationInterpreter();
        return specificationInterpreter.readSpecification(qualifiedSpec, endpoint, null);
    }


    @Test
    public void buildSimple() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("simpleFields.json", DATASET_ENDPOINT), COLLECTION_WHITELIST);

        String query = queryBuilderNew.build();

        System.out.println(query);
    }


    @Test
    public void buildSimpleTraverse() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("simpleTraverse.json", DATASET_ENDPOINT), COLLECTION_WHITELIST);

        String query = queryBuilderNew.build();

        System.out.println(query);
    }

    @Test
    public void buildSingleLevelNested() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("singleLevelNested.json", DATASET_ENDPOINT), COLLECTION_WHITELIST);

        String query = queryBuilderNew.build();

        System.out.println(query);
    }

    @Test
    public void buildMultiLevelNested() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("multiLevelNested.json", DATASET_ENDPOINT), COLLECTION_WHITELIST);

        String query = queryBuilderNew.build();

        System.out.println(query);
    }

    @Test
    public void buildMerge() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("merge.json", DATASET_ENDPOINT), COLLECTION_WHITELIST);
        String query = queryBuilderNew.build();
        System.out.println(query);
    }

    @Test
    public void buildMergeWithSubfields() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("mergeWithSubfields.json", DATASET_ENDPOINT), COLLECTION_WHITELIST);
        String query = queryBuilderNew.build();
        System.out.println(query);
    }

    @Test
    public void buildGroup() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("group.json", SPECIES_ENDPOINT), COLLECTION_WHITELIST);
        String query = queryBuilderNew.build();
        System.out.println(query);
    }



}