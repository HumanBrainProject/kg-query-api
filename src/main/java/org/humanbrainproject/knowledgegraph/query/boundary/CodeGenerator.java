package org.humanbrainproject.knowledgegraph.query.boundary;

import com.github.jsonldjava.utils.JsonUtils;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInternalRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
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
    SpecificationInterpreter specInterpreter;

    @Autowired
    JsonLdStandardization standardization;

    public String createPythonCode(StoredQueryReference queryReference) throws IOException, JSONException {
        String payload = arangoInternalRepository.getInternalDocumentByKey(new ArangoDocumentReference(SPECIFICATION_QUERIES, queryReference.getName()), String.class);
        Specification specification = specInterpreter.readSpecification(JsonUtils.toString(standardization.fullyQualify(payload)), queryReference.getSchemaReference(), null);
        specification.setSpecificationId(queryReference.getAlias());
        return pythonGenerator.generate(queryReference.getSchemaReference(), specification);
    }


}
