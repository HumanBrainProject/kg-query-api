package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;

import java.util.*;
import java.util.stream.Collectors;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

public class QueryBuilderNew {

    private final Specification specification;
    private final Pagination pagination;
    private final AuthorizedArangoQuery q;
    private final Map<String, Object> filterValues;
    private final Set<ArangoCollectionReference> existingCollections;


    public Map<String, Object> getFilterValues() {
        return filterValues;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public Specification getSpecification() {
        return specification;
    }

    public String build(List<String> restrictToIds, String search) {
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
        q.add(new FilterBuilder(rootAlias, specification.documentFilter, specification.fields).getFilter());

        q.addDocumentFilterWithWhitelistFilter(rootAlias);

        //FIXME We want to get rid of the static search parameter - this could be done dynamically but we keep it for backwards compatibility right now.
        if(search!=null){
            q.addLine(trust("FILTER ${rootFieldName}_doc.`http://schema.org/name` LIKE @searchQuery"));
            getFilterValues().put("searchQuery", "%"+search+"%");
        }

        if(restrictToIds!=null && !restrictToIds.isEmpty()){
            q.addLine(trust("FILTER ${rootFieldName}_doc._key IN @generalIdRestriction"));
            getFilterValues().put("generalIdRestriction", restrictToIds);
        }
        else {
            //Define sorting
            q.add(new SortBuilder(rootAlias, specification.fields).getSort());

            //Pagination
            if (this.pagination != null && this.pagination.getSize() != null) {
                q.addLine(trust("LIMIT ${paginationStart}, ${paginationSize}"));
                q.setParameter("paginationSize", String.valueOf(this.pagination.getSize()));
                q.setParameter("paginationStart", String.valueOf(this.pagination.getStart()));
            }
        }

        //Define return value
        q.add(new ReturnBuilder(rootAlias, null, specification.fields).getReturnStructure());

        return q.build().getValue();
    }


    public QueryBuilderNew(Specification specification, Set<String> permissionGroupsWithReadAccess, Pagination pagination, Map<String, String> filterValues, Set<ArangoCollectionReference> existingCollections) {
        this.q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        this.specification = specification;
        this.pagination = pagination;
        this.filterValues = new HashMap<>(filterValues);
        this.existingCollections = existingCollections;

    }

    public void defineRootInstance() {
        this.q.addLine(trust("FOR ${rootFieldName}_doc IN `${collection}`"));
    }

    static TrustedAqlValue getRepresentationOfField(ArangoAlias alias, SpecField field) {
        AQL representation = new AQL();
        if (field.isDirectChild()) {
            return representation.add(trust("${parentAlias}.`${originalKey}`")).setParameter("parentAlias", alias.getArangoDocName()).setParameter("originalKey", field.getLeafPath().pathName).build();
        } else if (field.hasGrouping()) {
            return representation.add(representation.preventAqlInjection(ArangoAlias.fromSpecField(field).getArangoName() + "_grp")).build();
        } else {
            return representation.add(representation.preventAqlInjection(ArangoAlias.fromSpecField(field).getArangoName())).build();
        }
    }


    private String getRootCollection() {
        return ArangoCollectionReference.fromNexusSchemaReference(NexusSchemaReference.createFromUrl(specification.rootSchema)).getName();
    }


    private class MergeBuilder {

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
                    if (mergeField.fields.size() == 1) {
                        merge.add(trust("${alias}_0"));
                    } else {
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


    private class TraverseBuilder {

        private final List<SpecField> fields;
        private final ArangoAlias parentAlias;


        public TraverseBuilder(ArangoAlias parentAlias, List<SpecField> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }

        private List<SpecField> fieldsWithTraversal() {
            return fields.stream().filter(SpecField::needsTraversal).filter(f -> !f.isMerge()).collect(Collectors.toList());
        }

        TrustedAqlValue handleTraverse(boolean firstTraverse, SpecTraverse traverse, ArangoAlias alias, Stack<ArangoAlias> aliasStack, boolean ensureOrder) {
            AQL aql = new AQL();
            aql.add(trust("LET ${alias} = "));
            if (!firstTraverse) {
                aql.add(trust("UNIQUE("));
            }
            aql.add(trust("FLATTEN("));

            if (traverseExists(traverse)) {

                aql.indent().add(trust("( LET ${aliasDoc}s = ( FOR ${aliasDoc}_traverse "));
                if (ensureOrder) {
                    aql.add(trust(", ${aliasDoc}_traverse_e"));
                }
                aql.addLine(trust(" IN 1..1 ${direction} ${parentAliasDoc} `${edgeCollection}`"));
                aql.indent().addDocumentFilterWithWhitelistFilter(trust(aql.preventAqlInjection(alias.getArangoDocName()).getValue() + "_traverse"));
                if (ensureOrder) {
                    aql.addLine(trust("SORT ${aliasDoc}_traverse_e." + ArangoVocabulary.ORDER_NUMBER + " ASC"));
                }
                aql.addLine(trust("RETURN ${aliasDoc}_traverse )"));
                if (traverse.reverse) {
                    //Reverse connections can not be embedded - we therefore can shortcut the query.
                    aql.addLine(trust("FOR ${aliasDoc} IN ${aliasDoc}s"));
                } else {
                    aql.addLine(trust("FOR ${aliasDoc} IN APPEND(${aliasDoc}s, FLATTEN([${parentAliasDoc}.`${fieldPath}`]))"));
                }
            } else {
                // the traverse collection does not exist - we therefore short-cut by only asking for the instances of a potentially embedded document.
                aql.addLine(trust("( FOR ${aliasDoc} IN FLATTEN([${parentAliasDoc}.`${fieldPath}`])"));
            }
            aql.addLine(trust("FILTER ${aliasDoc} != NULL"));
            aql.setParameter("alias", alias.getArangoName());
            aql.setParameter("aliasDoc", alias.getArangoDocName());
            aql.setParameter("direction", traverse.reverse ? "INBOUND" : "OUTBOUND");
            aql.setParameter("parentAliasDoc", aliasStack.peek().getArangoDocName());
            aql.setParameter("edgeCollection", ArangoCollectionReference.fromSpecTraversal(traverse).getName());
            aql.setParameter("fieldPath", traverse.pathName);
            return aql.build();
        }

        TrustedAqlValue finalizeTraversalWithSubfields(SpecField field, SpecTraverse lastTraverse, Stack<ArangoAlias> aliasStack) {
            AQL aql = new AQL();
            aql.addLine(new TraverseBuilder(aliasStack.peek(), field.fields).getTraversedField());
            return aql.build();
        }

        boolean traverseExists(SpecTraverse traverse) {
            return existingCollections.contains(ArangoCollectionReference.fromFieldName(traverse.pathName));
        }


        TrustedAqlValue getTraversedField() {
            List<SpecField> traversalFields = fieldsWithTraversal();
            if (!traversalFields.isEmpty()) {
                AQL fields = new AQL();
                for (SpecField traversalField : traversalFields) {
                    ArangoAlias traversalFieldAlias = ArangoAlias.fromSpecField(traversalField);
                    ArangoAlias alias = traversalFieldAlias;
                    Stack<ArangoAlias> aliasStack = new Stack<>();
                    aliasStack.push(parentAlias);
                    for (SpecTraverse traverse : traversalField.traversePath) {
                        boolean lastTraversal;
                        if (!traversalField.hasSubFields()) {
                            lastTraversal = traversalField.traversePath.size() < 2 || traverse == traversalField.traversePath.get(traversalField.traversePath.size() - 2);
                        } else {
                            lastTraversal = traverse == traversalField.traversePath.get(traversalField.traversePath.size() - 1);
                        }
                        fields.addLine(handleTraverse(traverse == traversalField.traversePath.get(0), traverse, alias, aliasStack, traversalField.ensureOrder));
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

                    fields.add(new FilterBuilder(alias, traversalField.fieldFilter, traversalField.fields).getFilter());
                    fields.add(new SortBuilder(alias, traversalField.fields).getSort());
                    fields.addLine(new ReturnBuilder(alias, traversalField, traversalField.fields).getReturnStructure());
                    while (aliasStack.size() > 1) {
                        ArangoAlias a = aliasStack.pop();
                        fields.addLine(trust("))"));
                        if (aliasStack.size() > 1) {
                            fields.addLine(trust(")"));
                            AQL returnStructure = new AQL();
                            if (traversalField.isSortAlphabetically()) {
                                returnStructure.addLine(trust("SORT ${traverseField} ASC"));
                            }
                            returnStructure.add(trust("RETURN DISTINCT ${traverseField}"));
                            returnStructure.setParameter("traverseField", a.getArangoName());
                            fields.addLine(returnStructure.build());
                        }
                    }
                    if (traversalField.hasGrouping()) {
                        handleGrouping(fields, traversalField, traversalFieldAlias);
                    }

                }
                return fields.build();
            } else {
                //return new ReturnBuilder(parentAlias, fields).getReturnStructure();
                return null;
            }
        }

        private void handleGrouping(AQL fields, SpecField traversalField, ArangoAlias traversalFieldAlias) {
            AQL group = new AQL();
            group.addLine(trust("LET ${traverseField}_grp = (FOR ${traverseField}_grp_inst IN ${traverseField}"));
            group.indent().add(trust("COLLECT "));

            List<SpecField> groupByFields = traversalField.fields.stream().filter(SpecField::isGroupby).collect(Collectors.toList());
            for (SpecField groupByField : groupByFields) {
                AQL groupField = new AQL();
                groupField.add(trust("`${field}` = ${traverseField}_grp_inst.`${field}`"));
                if (groupByField != groupByFields.get(groupByFields.size() - 1)) {
                    groupField.addComma();
                }
                groupField.setParameter("field", groupByField.fieldName);
                group.addLine(groupField.build());
            }
            group.addLine(trust("INTO ${traverseField}_group"));
            group.addLine(trust("LET ${traverseField}_instances = ( FOR ${traverseField}_group_el IN ${traverseField}_group"));
            List<SpecField> notGroupedByFields = traversalField.fields.stream().filter(f -> !f.isGroupby()).collect(Collectors.toList());

            sortNotGroupedFields(group, notGroupedByFields);

            group.addLine(trust("RETURN {"));

            for (SpecField notGroupedByField : notGroupedByFields) {
                AQL notGroupedField = new AQL();
                notGroupedField.add(trust("\"${field}\": ${traverseField}_group_el.${traverseField}_grp_inst.`${field}`"));
                if (notGroupedByField != notGroupedByFields.get(notGroupedByFields.size() - 1)) {
                    notGroupedField.addComma();
                }
                notGroupedField.setParameter("field", notGroupedByField.fieldName);
                group.addLine(notGroupedField.build());
            }

            group.addLine(trust("})"));

            sortGroupedFields(group, groupByFields);

            group.addLine(trust("RETURN {"));

            for (SpecField groupByField : groupByFields) {
                AQL groupField = new AQL();
                groupField.add(trust("\"${field}\": `${field}`"));
                groupField.addComma();
                groupField.setParameter("field", groupByField.fieldName);
                group.addLine(groupField.build());
            }
            group.addLine(trust("\"${collectField}\": ${traverseField}_instances"));
            group.addLine(trust("})"));
            group.setParameter("traverseField", traversalFieldAlias.getArangoName());
            group.setParameter("collectField", traversalField.groupedInstances);
            fields.addLine(group.build());
        }

        private void sortGroupedFields(AQL group, List<SpecField> groupByFields) {
            List<SpecField> groupedSortFields = groupByFields.stream().filter(f -> f.isSortAlphabetically()).collect(Collectors.toList());
            if (!groupedSortFields.isEmpty()) {
                group.add(trust("SORT "));
                for (SpecField specField : groupedSortFields) {
                    AQL groupSort = new AQL();
                    groupSort.add(trust("`${field}`"));
                    if (specField != groupedSortFields.get(groupedSortFields.size() - 1)) {
                        groupSort.addComma();
                    }
                    groupSort.setParameter("field", specField.fieldName);
                    group.add(groupSort.build());
                }
                group.add(trust(" ASC"));
            }
        }

        private void sortNotGroupedFields(AQL group, List<SpecField> notGroupedByFields) {
            List<SpecField> notGroupedSortFields = notGroupedByFields.stream().filter(SpecField::isSortAlphabetically).collect(Collectors.toList());
            if (!notGroupedSortFields.isEmpty()) {
                group.add(trust("SORT "));
                for (SpecField notGroupedSortField : notGroupedSortFields) {
                    AQL notGroupedSort = new AQL();
                    notGroupedSort.add(trust("${traverseField}_group_el.${traverseField}_grp_inst.`${field}`"));
                    if (notGroupedSortField != notGroupedByFields.get(notGroupedByFields.size() - 1)) {
                        notGroupedSort.addComma();
                    }
                    notGroupedSort.setParameter("field", notGroupedSortField.fieldName);
                    group.add(notGroupedSort.build());
                }
                group.addLine(trust(" ASC"));
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

            if (this.fields == null || this.fields.isEmpty()) {
                if (this.parentField != null) {
                    aql.addLine(trust("FILTER ${parentAliasDoc}.`${field}` != NULL"));
                }
            } else if (this.parentField != null) {
                aql.add(trust("FILTER "));
                for (SpecField field : fields) {
                    AQL fieldResult = new AQL();
                    fieldResult.add(trust("(${fieldRepresentation} != NULL AND ${fieldRepresentation} != [])"));
                    if (field != fields.get(fields.size() - 1)) {
                        fieldResult.add(trust(" OR "));
                    }
                    fieldResult.setTrustedParameter("fieldRepresentation", getRepresentationOfField(parentAlias, field));
                    aql.addLine(fieldResult.build());
                }
            }

            aql.add(trust("RETURN "));
            if (parentField != null) {
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


    private class FilterBuilder {

        private final List<SpecField> fields;
        private final ArangoAlias alias;
        private final FieldFilter parentFilter;

        public FilterBuilder(ArangoAlias alias, FieldFilter parentFilter, List<SpecField> fields) {
            this.fields = fields;
            this.alias = alias;
            this.parentFilter = parentFilter;
        }


        private List<SpecField> fieldsWithFilter() {
            return fields.stream().filter(f -> f.isRequired() || (f.fieldFilter != null && f.fieldFilter.getOp()!=null && !f.fieldFilter.getOp().isInstanceFilter())).collect(Collectors.toList());
        }

        TrustedAqlValue getFilter() {
            AQL filter = new AQL();
            filter.addDocumentFilter(alias);
            if(parentFilter!=null && parentFilter.getOp()!=null && parentFilter.getOp().isInstanceFilter()){
                filter.addLine(createInstanceFilter(parentFilter));
            }
            List<SpecField> fieldsWithFilter = fieldsWithFilter();
            if (!fieldsWithFilter.isEmpty()) {
                for (SpecField specField : fieldsWithFilter) {
                    filter.addLine(createFilter(specField));
                }
            }
            return filter.build();
        }

        private TrustedAqlValue createInstanceFilter(FieldFilter filter){
            AQL aql = new AQL();
            if (filter != null) {
                TrustedAqlValue fieldFilter = createFieldFilter(filter);
                if (fieldFilter != null) {
                    aql.addLine(trust("AND ${document} ${fieldFilter}"));
                    aql.setTrustedParameter("fieldFilter", fieldFilter);
                }
            }
            aql.setParameter("document", alias.getArangoDocName());
            return aql.build();
        }


        private TrustedAqlValue createFilter(SpecField field) {
            AQL aql = new AQL();
            if (field.isRequired()) {
                aql.addLine(trust("AND ${field} !=null"));
                aql.addLine(trust("AND ${field} !=\"\""));
                aql.addLine(trust("AND ${field} !=[]"));
            }
            if (field.fieldFilter != null && field.fieldFilter.getOp()!=null && !field.fieldFilter.getOp().isInstanceFilter()) {
                TrustedAqlValue fieldFilter = createFieldFilter(field.fieldFilter);
                if (fieldFilter != null) {
                    aql.addLine(trust("AND ${field}${fieldFilter}"));
                    aql.setTrustedParameter("fieldFilter", fieldFilter);
                }
            }
            aql.setTrustedParameter("field", getRepresentationOfField(alias, field));
            return aql.build();
        }


        private TrustedAqlValue createAqlForFilter(FieldFilter fieldFilter, boolean prefixWildcard, boolean postfixWildcard) {
            String value = null;
            String key = null;
            if (fieldFilter.getParameter() != null) {
                key = fieldFilter.getParameter().getName();
            } else {
                key = "staticFilter" + QueryBuilderNew.this.filterValues.size();
            }
            if (QueryBuilderNew.this.filterValues.containsKey(key)) {
                Object fromMap = QueryBuilderNew.this.filterValues.get(key);
                value = fromMap != null ? fromMap.toString() : null;
            }
            if (value == null && fieldFilter.getValue() != null) {
                value = fieldFilter.getValue().getValue();
            }
            if (value != null && key != null) {
                if (prefixWildcard && !value.startsWith("%")) {
                    value = "%" + value;
                }
                if (postfixWildcard && !value.endsWith("%")) {
                    value = value + "%";
                }
                QueryBuilderNew.this.filterValues.put(key, value);
                AQL aql = new AQL();
                aql.add(trust("@${field}"));
                aql.setParameter("field", key);
                return aql.build();
            }
            return null;
        }


        private TrustedAqlValue createFieldFilter(FieldFilter fieldFilter) {
            AQL aql = new AQL();
            TrustedAqlValue value;
            switch (fieldFilter.getOp()) {
                case REGEX:
                case EQUALS:
                case MBB:
                    value = createAqlForFilter(fieldFilter, false, false);
                    break;
                case STARTS_WITH:
                    value = createAqlForFilter(fieldFilter, false, true);
                    break;
                case ENDS_WITH:
                    value = createAqlForFilter(fieldFilter, true, false);
                    break;
                case CONTAINS:
                    value = createAqlForFilter(fieldFilter, true, true);
                    break;
                default:
                    value = null;
            }
            if (value != null) {
                switch (fieldFilter.getOp()) {
                    case EQUALS:
                        aql.add(trust(" == " + value.getValue()));
                        break;
                    case STARTS_WITH:
                    case ENDS_WITH:
                    case CONTAINS:
                        aql.add(trust(" LIKE " + value.getValue()));
                        break;
                    case REGEX:
                        aql.add(trust(" =~ " + value.getValue()));
                        break;
                    case MBB:
                        aql.add(trust("._id IN " + value.getValue()+" "));
                        break;
                }
                return aql.build();
            }
            return null;
        }
    }

    public static List<String> createIdRestriction(Set<ArangoDocumentReference> references){
        if(references==null || references.isEmpty()){
            return null;
        }
        return references.stream().map(ArangoDocumentReference::getId).collect(Collectors.toList());

    }
}
