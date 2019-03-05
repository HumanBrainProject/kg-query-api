package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

public abstract class AbstractReleaseTreeBuilder {

    AQL createReleaseStatusQuery(ArangoAlias alias, String nexusInstanceBase) {
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

}
