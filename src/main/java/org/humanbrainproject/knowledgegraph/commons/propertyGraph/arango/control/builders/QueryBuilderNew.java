package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

public class QueryBuilderNew {

    private final Specification specification;
    private final AuthorizedArangoQuery q;


    public String build() {
        //Define the global parameters
        ArangoAlias rootAlias = new ArangoAlias("root");
        q.setParameter("rootFieldName", rootAlias.getArangoName());
        q.setParameter("collection", getRootCollection());


        //Setup the root instance
        defineRootInstance();

        q.add(new MergeBuilder(rootAlias, specification.fields).getMergedFields());

        //Define the complex fields (the ones with traversals)
        q.add(new TraverseBuilder(rootAlias, specification.fields).getTraversedField());

        //Define filters
        q.add(new FilterBuilder(rootAlias, specification.fields).getFilter());

        //Define sorting
        q.add(new SortBuilder(rootAlias, specification.fields).getSort());

        //Define return value
        q.add(new ReturnBuilder(rootAlias, null, specification.fields).getReturnStructure());

        return q.build().getValue();
    }


    public QueryBuilderNew(Specification specification, Set<String> permissionGroupsWithReadAccess) {
        this.q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        this.specification = specification;
    }

    public void defineRootInstance() {
        this.q.addLine(trust("FOR ${rootFieldName}_doc IN `${collection}`"));
    }

    static TrustedAqlValue getRepresentationOfField(ArangoAlias alias, SpecField field) {
        AQL representation = new AQL();
        if (field.isDirectChild()) {
            return representation.add(trust("${parentAlias}.`${originalKey}`")).setParameter("parentAlias", alias.getArangoDocName()).setParameter("originalKey", field.getLeafPath().pathName).build();
        } else {
            return representation.add(representation.preventAqlInjection(ArangoAlias.fromSpecField(field).getArangoName())).build();
        }
    }


    private String getRootCollection() {
        return ArangoCollectionReference.fromNexusSchemaReference(NexusSchemaReference.createFromUrl(specification.rootSchema)).getName();
    }

    private static class MergeBuilder {

        private final List<SpecField> fields;
        private final ArangoAlias parentAlias;

        public MergeBuilder(ArangoAlias parentAlias, List<SpecField> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }

        private List<SpecField> fieldsWithMerge() {
            return fields.stream().filter(SpecField::isMerge).collect(Collectors.toList());
        }

        TrustedAqlValue getMergedFields() {
            List<SpecField> mergeFields = fieldsWithMerge();
            if (!mergeFields.isEmpty()) {
                AQL aql = new AQL();
                for (SpecField mergeField : mergeFields) {
                    aql.add(new TraverseBuilder(parentAlias, mergeField.fields).getTraversedField());
                    AQL merge = new AQL();
                    merge.add(trust("LET ${alias} = "));
                    if(mergeField.fields.size()==1){
                        merge.add(trust("${alias}_0"));
                    }
                    else {
                        for (int i = 0; i < mergeField.fields.size(); i++) {
                            AQL append = new AQL();
                            if (i < mergeField.fields.size() - 1) {
                                append.add(trust("(APPEND(${field},"));
                            } else {
                                append.add(trust("${field}"));
                            }
                            append.setParameter("field", ArangoAlias.fromSpecField(mergeField).getArangoName() + "_" + i);
                            merge.add(append.build());
                        }
                        for (SpecField field : mergeField.fields) {
                            merge.add(trust(")"));
                        }
                    }
                    merge.setParameter("alias", ArangoAlias.fromSpecField(mergeField).getArangoName());
                    aql.addLine(merge.build());
                }
                return aql.build();
            }

            return null;
        }


    }


    private static class TraverseBuilder {

        private final List<SpecField> fields;
        private final ArangoAlias parentAlias;


        public TraverseBuilder(ArangoAlias parentAlias, List<SpecField> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }

        private List<SpecField> fieldsWithTraversal() {
            return fields.stream().filter(SpecField::needsTraversal).filter(f -> !f.isMerge()).collect(Collectors.toList());
        }

        TrustedAqlValue handleTraverse(SpecTraverse traverse, ArangoAlias alias, Stack<ArangoAlias> aliasStack, boolean ensureOrder) {
            AQL aql = new AQL();
            aql.add(trust("LET ${alias} = UNIQUE(FLATTEN(FOR ${aliasDoc}"));
            if (ensureOrder) {
                aql.add(trust(", ${aliasDoc}_e"));
            }
            aql.addLine(trust(" IN 1..1 ${direction} ${parentAliasDoc} `${edgeCollection}`"));
            aql.indent().addDocumentFilterWithWhitelistFilter(alias);
            if (ensureOrder) {
                aql.addLine(trust("SORT ${aliasDoc}_e." + ArangoVocabulary.ORDER_NUMBER + " ASC"));
            }
            aql.setParameter("alias", alias.getArangoName());
            aql.setParameter("aliasDoc", alias.getArangoDocName());
            aql.setParameter("direction", traverse.reverse ? "INBOUND" : "OUTBOUND");
            aql.setParameter("parentAliasDoc", aliasStack.peek().getArangoDocName());
            aql.setParameter("edgeCollection", ArangoCollectionReference.fromSpecTraversal(traverse).getName());
            return aql.build();
        }

        TrustedAqlValue finalizeTraversalWithSubfields(SpecField field, SpecTraverse lastTraverse, Stack<ArangoAlias> aliasStack) {
            AQL aql = new AQL();
            aql.addLine(new TraverseBuilder(aliasStack.peek(), field.fields).getTraversedField());
            return aql.build();
        }


        TrustedAqlValue getTraversedField() {
            List<SpecField> traversalFields = fieldsWithTraversal();
            if (!traversalFields.isEmpty()) {
                AQL fields = new AQL();
                for (SpecField traversalField : traversalFields) {
                    ArangoAlias alias = ArangoAlias.fromSpecField(traversalField);
                    Stack<ArangoAlias> aliasStack = new Stack<>();
                    aliasStack.push(parentAlias);
                    for (SpecTraverse traverse : traversalField.traversePath) {
                        boolean lastTraversal;
                        if (!traversalField.hasSubFields()) {
                            lastTraversal = traversalField.traversePath.size() < 2 || traverse == traversalField.traversePath.get(traversalField.traversePath.size() - 2);
                        } else {
                            lastTraversal = traverse == traversalField.traversePath.get(traversalField.traversePath.size() - 1);
                        }
                        fields.addLine(handleTraverse(traverse, alias, aliasStack, traversalField.ensureOrder));
                        aliasStack.push(alias);
                        if (lastTraversal) {
                            if (traversalField.hasSubFields()) {
                                fields.add(finalizeTraversalWithSubfields(traversalField, traverse, aliasStack));
                            }
                        }
                        if (lastTraversal) {
                            break;
                        }
                        alias = alias.increment();
                    }

                    fields.add(new FilterBuilder(alias, traversalField.fields).getFilter());
                    fields.add(new SortBuilder(alias, traversalField.fields).getSort());
                    fields.addLine(new ReturnBuilder(alias, traversalField, traversalField.fields).getReturnStructure());
                    while (aliasStack.size() > 1) {
                        ArangoAlias a = aliasStack.pop();
                        fields.addLine(trust("))"));
                        if (aliasStack.size() > 1) {
                            AQL returnStructure = new AQL();
                            returnStructure.add(trust("RETURN DISTINCT ${traverseField}"));
                            returnStructure.setParameter("traverseField", a.getArangoName());
                            fields.addLine(returnStructure.build());
                        }
                    }
                }
                return fields.build();
            } else {
                //return new ReturnBuilder(parentAlias, fields).getReturnStructure();
                return null;
            }
        }

    }


    private static class ReturnBuilder {

        private final List<SpecField> fields;
        private final ArangoAlias parentAlias;
        private final SpecField parentField;

        public ReturnBuilder(ArangoAlias parentAlias, SpecField parentField, List<SpecField> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
            this.parentField = parentField;
        }

        TrustedAqlValue getReturnStructure() {
            AQL aql = new AQL();
            aql.add(trust("RETURN "));
            if(parentField!=null){
                aql.add(trust("DISTINCT "));
            }

            if (this.fields == null || this.fields.isEmpty()) {
                if (this.parentField != null) {
                    aql.add(trust("${parentAliasDoc}.`${field}`"));
                    aql.setParameter("parentAliasDoc", parentAlias.getArangoDocName());
                    aql.setParameter("field", parentField.getLeafPath().pathName);
                }
            } else {
                aql.indent();
                aql.add(trust("{"));
                for (SpecField field : fields) {
                    AQL fieldResult = new AQL();
                    fieldResult.add(new TrustedAqlValue("\"${fieldName}\": ${fieldRepresentation}"));
                    fieldResult.setParameter("fieldName", field.fieldName);
                    fieldResult.setTrustedParameter("fieldRepresentation", getRepresentationOfField(parentAlias, field));
                    if (field != fields.get(fields.size() - 1)) {
                        fieldResult.addComma();
                    }
                    aql.addLine(fieldResult.build());
                }
                aql.outdent();
                aql.addLine(trust("}"));
            }
            return aql.build();
        }


    }


    private static class SortBuilder {

        private final List<SpecField> fields;
        private final ArangoAlias parentAlias;

        public SortBuilder(ArangoAlias parentAlias, List<SpecField> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }


        private List<SpecField> fieldsWithSort() {
            return fields.stream().filter(SpecField::isSortAlphabetically).collect(Collectors.toList());
        }

        TrustedAqlValue getSort() {
            List<SpecField> sortFields = fieldsWithSort();
            if (!sortFields.isEmpty()) {
                AQL aql = new AQL();
                aql.add(trust("SORT "));
                for (SpecField sortField : sortFields) {
                    AQL sort = new AQL();
                    if (sortField != sortFields.get(0)) {
                        sort.addComma();
                    }
                    sort.add(trust("${field}"));
                    sort.setTrustedParameter("field", getRepresentationOfField(parentAlias, sortField));
                    aql.add(sort.build());
                }
                aql.addLine(trust(" ASC"));
                return aql.build();
            }
            return null;
        }


    }


    private static class FilterBuilder {

        private final List<SpecField> fields;
        private final ArangoAlias alias;

        public FilterBuilder(ArangoAlias alias, List<SpecField> fields) {
            this.fields = fields;
            this.alias = alias;
        }


        private List<SpecField> fieldsWithFilter() {
            return fields.stream().filter(f -> f.isRequired() || f.fieldFilter != null).collect(Collectors.toList());
        }

        TrustedAqlValue getFilter() {
            List<SpecField> fieldsWithFilter = fieldsWithFilter();
            if (!fieldsWithFilter.isEmpty()) {
                AQL filter = new AQL();
                filter.addDocumentFilter(alias);
                for (SpecField specField : fieldsWithFilter) {
                    filter.addLine(createFilter(specField));
                }
                return filter.build();
            }
            return null;
        }

        private TrustedAqlValue createFilter(SpecField field) {
            AQL aql = new AQL();
            if (field.isRequired()) {
                aql.addLine(trust("AND ${field} !=null"));
                aql.addLine(trust("AND ${field} !=\"\""));
                aql.addLine(trust("AND ${field} !=[]"));
            }
            if (field.fieldFilter != null && field.fieldFilter.getExpAsValue() != null && field.fieldFilter.getOp() != null) {
                aql.addLine(trust("AND ${field} ${fieldFilter}"));
                aql.setTrustedParameter("fieldFilter", createFieldFilter(field));
            }
            aql.setTrustedParameter("field", getRepresentationOfField(alias, field));
            return aql.build();
        }

        private TrustedAqlValue createFieldFilter(SpecField field) {
            FieldFilter fieldFilter = field.fieldFilter;
            AQL aql = new AQL();
            switch (fieldFilter.getOp()) {
                case EQUALS:
                    aql.add(trust("== \"${value}\""));
                    aql.setParameter("value", fieldFilter.getExpAsValue().getValue());
                    break;
                case LIKE:
                    aql.add(trust("LIKE \"${value}\""));
                    aql.setTrustedParameter("value", aql.generateSearchTermQuery(aql.preventAqlInjection(fieldFilter.getExpAsValue().getValue())));
                    break;
            }
            return aql.build();
        }
    }
}
