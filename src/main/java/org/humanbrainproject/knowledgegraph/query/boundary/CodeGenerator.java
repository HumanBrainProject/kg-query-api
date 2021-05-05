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

package org.humanbrainproject.knowledgegraph.query.boundary;

import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.control.PythonGenerator;
import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.StoredQueryReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class CodeGenerator {

    public static final ArangoCollectionReference SPECIFICATION_QUERIES = new ArangoCollectionReference("specification_queries");


    @Autowired
    ArangoInternalRepository arangoInternalRepository;

    @Autowired
    PythonGenerator pythonGenerator;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    SpecificationInterpreter specInterpreter;

    @Autowired
    JsonLdStandardization standardization;

    public String createPythonCode(StoredQueryReference queryReference) throws IOException, JSONException {
        String payload = arangoInternalRepository.getInternalDocumentByKey(new ArangoDocumentReference(SPECIFICATION_QUERIES, queryReference.getName()), String.class);
        if(payload!=null){
            NexusSchemaReference schemaReference = queryReference.getSchemaReference();
            Specification specification = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(payload)), schemaReference != null ? nexusConfiguration.getAbsoluteUrl(schemaReference) : null, null);
            specification.setSpecificationId(queryReference.getAlias());
            return pythonGenerator.generate(queryReference.getSchemaReference(), specification);
        }
        return null;
    }


}
