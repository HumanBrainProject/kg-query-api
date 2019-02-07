package org.humanbrainproject.knowledgegraph.query.control;

import org.apache.commons.lang3.StringUtils;
import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.PythonClass;
import org.humanbrainproject.knowledgegraph.query.entity.PythonField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PythonGenerator {

    @Autowired
    SemanticsToHumanTranslator semanticsToHumanTranslator;

    public String generate(NexusSchemaReference schemaReference, Specification specification) {
        StringBuilder sb = new StringBuilder();
        sb.append("from typing import Sequence\n");
        sb.append("\n");
        sb.append("from kgquery.queryApi import Query, KGClient");
        sb.append("\n");
        sb.append("\n");

        List<PythonClass> pythonClasses = new ArrayList<>();

        extractPythonClasses(pythonClasses, specification.fields, schemaReference.getSchema());



        for (PythonClass pythonClass : pythonClasses) {
            sb.append(String.format("\nclass %s:\n", pythonClass.getName()));
            for (PythonField pythonField : pythonClass.getFields()) {
                sb.append(String.format("    %s: %s\n", pythonField.getName(), pythonField.getType()!=null ? String.format("Sequence[%s]", pythonField.getType()) : "any"));
            }
            sb.append("\n");
            String fieldName = getPythonFieldName(pythonClass.getName());
            sb.append(String.format("\ndef _%s_from_payload(payload: dict) -> %s:\n", fieldName, pythonClass.getName()));
            sb.append(String.format("    %s = %s()", fieldName, pythonClass.getName()));
            for (PythonField pythonField : pythonClass.getFields()) {
                if(pythonField.getType()==null) {
                    sb.append(String.format("\n    %s.%s = payload[\"%s\"] if \"%s\" in payload else None", fieldName, pythonField.getName(), pythonField.getKey(), pythonField.getKey()));
                }
                else{
                    sb.append(String.format("\n    %s.%s = []\n", fieldName, pythonField.getName()));
                    sb.append(String.format("    if \"%s\" in payload and isinstance(payload[\"%s\"], list):\n", pythonField.getKey(), pythonField.getKey()));
                    sb.append(String.format("        for c in payload[\"%s\"]:\n", pythonField.getKey()));
                    sb.append(String.format("            %s.%s.append(_%s_from_payload(c))" +
                            "", fieldName, pythonField.getName(), semanticsToHumanTranslator.simplePluralToSingular(pythonField.getName()).toLowerCase()));
                }
            }
            sb.append(String.format("\n    return %s", fieldName));
            sb.append("\n\n");
        }

        String rootClassName = StringUtils.capitalize(semanticsToHumanTranslator.simplePluralToSingular(schemaReference.getSchema()));
        sb.append(String.format("\nclass %s(Query[%s]):\n\n", StringUtils.capitalize(specification.getSpecificationId())+rootClassName, rootClassName));
        sb.append("    def __init__(self, client: KGClient):\n");
        sb.append(String.format("        super().__init__(client, \"%s\", \"%s\")\n\n", schemaReference.getRelativeUrl().getUrl(), specification.getSpecificationId()));

        sb.append(String.format("    def create_result(self, payload: dict) -> %s:\n", rootClassName));
        sb.append(String.format("        return _%s_from_payload(payload)\n\n", rootClassName.toLowerCase()));

        return sb.toString();

    }

    public void extractPythonClasses(List<PythonClass> classes, List<SpecField> fields, String name) {
        if (!fields.isEmpty()) {
            List<PythonField> pythonFields = new ArrayList<>();
            for (SpecField field : fields) {
                String pythonFieldName = getPythonFieldName(field.fieldName);
                List<SpecField> fieldsToBeProcessed = field.fields;
                if(field.isMerge()){
                    fieldsToBeProcessed = new ArrayList<>();
                    Set<String> handledFields = new HashSet<>();
                    for (SpecField specField : field.fields) {
                        for (SpecField subfield : specField.fields) {
                            if(!handledFields.contains(subfield.fieldName)){
                                fieldsToBeProcessed.add(subfield);
                                handledFields.add(subfield.fieldName);
                            }
                        }
                    }
                }
                extractPythonClasses(classes, fieldsToBeProcessed, semanticsToHumanTranslator.extractSimpleAttributeName(field.fieldName));
                pythonFields.add(new PythonField(pythonFieldName, fieldsToBeProcessed.isEmpty() ? null : StringUtils.capitalize(semanticsToHumanTranslator.simplePluralToSingular(pythonFieldName)), field.fieldName));
            }
            PythonClass python = new PythonClass(StringUtils.capitalize(semanticsToHumanTranslator.simplePluralToSingular(name)), pythonFields);
            classes.add(python);
        }
    }


    private String getPythonFieldName(String fieldName) {
        String label = semanticsToHumanTranslator.extractSimpleAttributeName(fieldName);
        return label.replaceAll("[^\\sA-Za-z0-9]", "").replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();

    }


}
