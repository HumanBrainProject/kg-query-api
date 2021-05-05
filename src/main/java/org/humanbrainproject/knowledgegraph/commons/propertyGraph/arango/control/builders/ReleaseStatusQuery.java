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

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

public class ReleaseStatusQuery {


    public static AQL createReleaseStatusQuery(ArangoAlias alias, String nexusInstanceBase) {
        AQL releaseStatusQuery = new AQL();
        releaseStatusQuery.addLine(trust("LET ${name}_release = (FOR ${name}_status_doc IN 1..1 INBOUND ${name}_doc `${releaseInstanceRelation}`"));
        releaseStatusQuery.addLine(trust("FILTER ${name}_status_doc != NULL"));
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
