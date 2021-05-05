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

    Map<String, String> parameters;

    @Before
    public void setup() {
        this.pagination = new Pagination();
        this.pagination.setSize(25);
        this.parameters = new HashMap<>();
        this.existingCollections = new HashSet<>();
    }

    private void printResult(DataQueryBuilder queryBuilderNew){
        System.out.println(queryBuilderNew.build(null, null));
        System.out.println("Bind parameters:");
        for (String key : queryBuilderNew.getProcessedFilterValues().keySet()) {
            System.out.println(key+": "+queryBuilderNew.getProcessedFilterValues().get(key));
        }
    }

    private Specification ensureAllTraversalFieldsInCollectionList(Specification specification){
        ensureAllTraversalFieldsInExistingCollections(specification.getFields());
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
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(readSpecification("simpleFields.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, null, pagination, parameters, existingCollections);
        printResult(queryBuilderNew);
    }


    @Test
    public void buildSimpleTraverse() throws IOException, JSONException {
        Specification specification = readSpecification("simpleTraverse.json", DATASET_ENDPOINT);
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(specification, COLLECTION_WHITELIST,null, pagination, parameters, existingCollections);
        printResult(queryBuilderNew);

    }

    @Test
    public void buildSingleLevelNested() throws IOException, JSONException {
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(readSpecification("singleLevelNested.json", DATASET_ENDPOINT), COLLECTION_WHITELIST,null, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildMultiLevelNested() throws IOException, JSONException {
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(readSpecification("multiLevelNested.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, null,pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildMerge() throws IOException, JSONException {
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(readSpecification("merge.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, null,pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildMergeWithSubfields() throws IOException, JSONException {
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(readSpecification("mergeWithSubfields.json", DATASET_ENDPOINT), COLLECTION_WHITELIST, null,pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildGroup() throws IOException, JSONException {
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(readSpecification("group.json", SPECIES_ENDPOINT), COLLECTION_WHITELIST, null,pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }


    @Test
    public void buildEmbedded() throws IOException, JSONException {
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(readSpecification("embedded.json", ORGANIZATION_ENDPOINT), COLLECTION_WHITELIST, null,pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }

    @Test
    public void buildFilter() throws IOException, JSONException {
        parameters.put("dynamicRegex", ".*Probab.*");
        parameters.put("dynamicOverride", "Whole.*");
        DataQueryBuilder queryBuilderNew = new DataQueryBuilder(readSpecification("filter.json", DATASET_ENDPOINT), COLLECTION_WHITELIST,null, pagination, parameters, existingCollections);

        printResult(queryBuilderNew);
    }


}