package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

public class ReflectionBuilder {

    private final Specification specification;
    private final ArangoDocumentReference instanceId;
    private final AuthorizedArangoQuery q;
    private final Stack<ArangoAlias> aliasStack;
    private final String nexusInstanceBase;

    private final Set<ArangoCollectionReference> existingCollections;

    private int internalFieldCounter = 0;
    private final ArangoAlias rootAlias = new ArangoAlias("root");

    public ReflectionBuilder(Specification specification, Set<String> permissionGroupsWithReadAccess, ArangoDocumentReference instanceId, Set<ArangoCollectionReference> existingCollections, String nexusInstanceBase) {
        this.q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        this.specification = specification;
        this.instanceId = instanceId;
        this.existingCollections = existingCollections;
        this.aliasStack = new Stack<>();
        this.nexusInstanceBase = nexusInstanceBase;
    }

    public String build() {
        q.setParameter("rootFieldName", rootAlias.getArangoName());
        q.setParameter("rootDoc", rootAlias.getArangoDocName());
        q.setParameter("collection", getRootCollection());
        q.setParameter("instanceId", instanceId.getKey());
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//add root"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        q.addLine(trust("FOR ${rootDoc} IN `${collection}`")).indent();
        q.addDocumentFilter(rootAlias);
        q.addLine(createReleaseStatusQuery(rootAlias).build());
        q.addLine(trust("FILTER ${rootDoc}._id == \"${id}\""));
        List<ArangoAlias> fieldAliases = processFields(rootAlias, specification.getFields());
        q.addLine(trust("RETURN {"));
        q.setParameter("id", instanceId.getId());
        q.addLine(trust(" \"" + JsonLdConsts.ID + "\": ${rootDoc}.`" + JsonLdConsts.ID + "`,"));
        q.addLine(trust(" \"" + SchemaOrgVocabulary.NAME + "\": ${rootDoc}.`" + SchemaOrgVocabulary.NAME + "`,"));
        q.addLine(trust(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${rootDoc}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
        handleReturnStructureOfSubfields(q, fieldAliases);
        q.addLine(trust(" \"status\": ${rootDoc}_status,"));
        q.addLine(trust(" \"" + JsonLdConsts.TYPE + "\": ${rootDoc}.`" + JsonLdConsts.TYPE + "`"));
        q.addLine(trust("}"));

        return q.build().getValue();
    }



    private AQL createReleaseStatusQuery(ArangoAlias alias) {
        AQL releaseStatusQuery = new AQL();
        releaseStatusQuery.addLine(trust("LET ${name}_release = (FOR ${name}_status_doc IN 1..1 INBOUND ${name}_doc `${releaseInstanceRelation}`"));
        releaseStatusQuery.addLine(trust("LET ${name}_release_instance = SUBSTITUTE(CONCAT(${name}_status_doc.`${releaseInstanceProperty}`.`" + JsonLdConsts.ID + "`, \"?rev=\", ${name}_status_doc.`${releaseRevisionProperty}`), \"${nexusBaseForInstances}/\", \"\")"));
        releaseStatusQuery.addLine(trust("RETURN ${name}_release_instance==${name}_doc.${originalId} ? \"${releasedValue}\" : \"${changedValue}\""));
        releaseStatusQuery.addLine(trust(")"));
        releaseStatusQuery.addLine(trust("LET ${name}_doc_status = LENGTH(${name}_release)>0 ? ${name}_release[0] : \"${notReleasedValue}\""));
        releaseStatusQuery.setParameter("name", alias.getArangoName());
        releaseStatusQuery.setParameter("releaseInstanceRelation", ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE).getName());
        releaseStatusQuery.setParameter("releaseInstanceProperty", HBPVocabulary.RELEASE_INSTANCE);
        releaseStatusQuery.setParameter("releaseRevisionProperty", HBPVocabulary.RELEASE_REVISION);
        releaseStatusQuery.setParameter("nexusBaseForInstances", nexusInstanceBase);
        releaseStatusQuery.setParameter("originalId", ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV);
        releaseStatusQuery.setParameter("releasedValue", ReleaseStatus.RELEASED.name());
        releaseStatusQuery.setParameter("changedValue", ReleaseStatus.HAS_CHANGED.name());
        releaseStatusQuery.setParameter("notReleasedValue", ReleaseStatus.NOT_RELEASED.name());
        return releaseStatusQuery;
    }


    private List<ArangoAlias> processFields(ArangoAlias originalAlias, List<SpecField> fields) {
        List<ArangoAlias> result = new ArrayList<>();
        for (SpecField field : fields) {
            if (field.needsTraversal()) {
                Stack<ArangoAlias> aliasStack = new Stack<>();
                Stack<SpecTraverse> traverseStack = new Stack<>();
                for (SpecTraverse traverse : field.traversePath) {
                    ArangoCollectionReference collection = ArangoCollectionReference.fromSpecTraversal(traverse);
                    if(existingCollections.contains(collection)) {
                        ArangoAlias alias = new ArangoAlias(ArangoAlias.fromLeafPath(traverse).getArangoName() + "_" + internalFieldCounter);
                        AQL subQuery = new AQL();
                        subQuery.addLine(trust("//Adding ${alias}"));
                        subQuery.addLine(trust("LET ${alias} = ("));
                        subQuery.addLine(trust("FOR ${aliasDoc} IN 1..1 ${inOutBound} ${previousDoc} `${relation}`"));
                        subQuery.addLine(createReleaseStatusQuery(alias).build());
                        subQuery.setParameter("alias", alias.getArangoName());
                        subQuery.setParameter("aliasDoc", alias.getArangoDocName());
                        subQuery.setParameter("inOutBound", traverse.reverse ? "INBOUND" : "OUTBOUND");
                        subQuery.setParameter("previousDoc", aliasStack.empty() ? originalAlias.getArangoDocName() : aliasStack.peek().getArangoDocName());
                        subQuery.setParameter("relation", ArangoCollectionReference.fromSpecTraversal(traverse).getName());
                        subQuery.addDocumentFilter(alias);
                        aliasStack.push(alias);
                        traverseStack.push(traverse);
                        q.addLine(subQuery.build());
                    }
                    else{
                        break;
                    }
                }
                ArangoAlias previousAlias = null;
                while(!traverseStack.empty()){
                    SpecTraverse traverse = traverseStack.pop();
                    ArangoAlias alias = aliasStack.pop();
                    AQL subQuery = new AQL();
                    subQuery.addLine(trust("RETURN DISTINCT {"));
                    subQuery.addLine(trust(" \"" + JsonLdConsts.ID + "\": ${aliasDoc}.`" + JsonLdConsts.ID + "`,"));
                    subQuery.addLine(trust(" \"" + SchemaOrgVocabulary.NAME + "\": ${aliasDoc}.`" + SchemaOrgVocabulary.NAME + "`,"));
                    subQuery.addLine(trust(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${aliasDoc}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
                    if(previousAlias!=null){
                        subQuery.addLine(trust(" \"children\":${previousAlias},"));
                        subQuery.setParameter("previousAlias", previousAlias.getArangoName());
                    }
                    else if(field.hasSubFields()){
                        List<ArangoAlias> fieldAliases = processFields(alias, field.fields);
                        handleReturnStructureOfSubfields(subQuery, fieldAliases);
                    }
                    subQuery.addLine(trust(" \"status\": ${aliasDoc}_status,"));
                    subQuery.addLine(trust(" \"" + JsonLdConsts.TYPE + "\": ${aliasDoc}.`" + JsonLdConsts.TYPE + "`"));
                    subQuery.addLine(trust("})"));
                    subQuery.setParameter("aliasDoc", alias.getArangoDocName());
                    q.addLine(subQuery.build());
                    previousAlias = alias;
                }
                internalFieldCounter++;
                if(previousAlias!=null) {
                    result.add(previousAlias);
                }
            }
        }
        return result;
    }

    private void handleReturnStructureOfSubfields(AQL query, List<ArangoAlias> fieldAliases) {
        if(fieldAliases!=null && !fieldAliases.isEmpty()){
            query.addLine(trust(" \"children\": UNION_DISTINCT("));
            for (ArangoAlias fieldAlias : fieldAliases) {
                query.addLine(new AQL().add(trust("${a},")).setParameter("a", fieldAlias.getArangoName()).build());
            }
            query.addLine(trust("[]),"));
        }
    }


    private String getRootCollection() {
        return ArangoCollectionReference.fromNexusSchemaReference(NexusSchemaReference.createFromUrl(specification.getRootSchema())).getName();
    }


}
