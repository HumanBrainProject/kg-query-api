package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.*;

public class QueryBuilderNewTest {

    private final static Set<String> COLLECTION_WHITELIST = new HashSet<>(Arrays.asList("minds", "neuralactivity", "foo", "bar", "cscs"));

    private final static String DATASET_ENDPOINT = "http://test/v0/minds/core/dataset/v1.0.0";
    private final static String SPECIES_ENDPOINT = "http://test/v0/minds/core/species/v1.0.0";
    private final static String ORGANIZATION_ENDPOINT = "http://test/v0/neuralactivity/core/organization/v0.1.0";

    Set<ArangoCollectionReference> existingCollections;

    Pagination pagination;

    Map<String, Object> parameters;

    @Before
    public void setup() {
        this.pagination = new Pagination();
        this.pagination.setSize(25);
        this.parameters = new HashMap<>();
        this.existingCollections = new HashSet<>();
    }

    private void printResult(QueryBuilderNew queryBuilderNew){
        System.out.println(queryBuilderNew.build(null, null));
        System.out.println("Bind parameters:");
        for (String key : queryBuilderNew.getFilterValues().keySet()) {
            System.out.println(key+": "+queryBuilderNew.getFilterValues().get(key));
        }
    }

    private Specification ensureAllTraversalFieldsInCollectionList(Specification specification){
        ensureAllTraversalFieldsInExistingCollections(specification.fields);
        return specification;
    }

    private void ensureAllTraversalFieldsInExistingCollections(List<SpecField> fields){
        for (SpecField field : fields) {
            if(field.needsTraversal()){
                for (SpecTraverse traverse : field.traversePath) {
                    this.existingCollections.add(ArangoCollectionReference.fromSpecTraversal(traverse));
                }
            }
            if(field.hasSubFields()){
                ensureAllTraversalFieldsInExistingCollections(field.fields);
            }
        }
    }


    private Specification readSpecification(String fileName, String endpoint) throws IOException, JSONException {
        String specification = IOUtils.toString(this.getClass().getResourceAsStream("/apiSpec/" + fileName), "UTF-8");
        JsonTransformer jsonTransformer = new JsonTransformer();
        String qualifiedSpec = jsonTransformer.getMapAsJson(new JsonLdStandardization().fullyQualify(jsonTransformer.parseToMap(specification)));
        SpecificationInterpreter specificationInterpreter = new SpecificationInterpreter();
        return ensureAllTraversalFieldsInCollectionList(specificationInterpreter.readSpecification(qualifiedSpec, endpoint, null));
    }


    @Test
    public void buildSimple() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("simpleFields.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, pagination, parameters, existingCollections);
        printResult(queryBuilderNew);
    }


    @Test
    public void buildSimpleTraverse() throws IOException, JSONException {
        Specification specification = readSpecification("simpleTraverse.json", DATASET_ENDPOINT);
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(specification, COLLECTION_WHITELIST, pagination, parameters, existingCollections);
        printResult(queryBuilderNew);

    }

    @Test
    public void buildSingleLevelNested() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("singleLevelNested.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildMultiLevelNested() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("multiLevelNested.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildMerge() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("merge.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildMergeWithSubfields() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("mergeWithSubfields.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildGroup() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("group.json", SPECIES_ENDPOINT), COLLECTION_WHITELIST, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }


    @Test
    public void buildEmbedded() throws IOException, JSONException {
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("embedded.json", ORGANIZATION_ENDPOINT), COLLECTION_WHITELIST, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildFilter() throws IOException, JSONException {
        parameters.put("dynamicRegex", ".*Probab.*");
        parameters.put("dynamicOverride", "Whole.*");
        QueryBuilderNew queryBuilderNew = new QueryBuilderNew(readSpecification("filter.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }


}