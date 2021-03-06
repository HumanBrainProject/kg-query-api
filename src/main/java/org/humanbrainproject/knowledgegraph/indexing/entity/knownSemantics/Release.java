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

package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;

import java.util.Map;

@ToBeTested(easy = true)
public class Release extends KnownSemantic {

    public Release(QualifiedIndexingMessage spec) {
        super(spec, HBPVocabulary.RELEASE_TYPE);
    }

    public NexusInstanceReference getReleaseInstance(){
        Object releaseInstance = this.spec.getQualifiedMap().get(HBPVocabulary.RELEASE_INSTANCE);
        if(releaseInstance instanceof Map && ((Map)releaseInstance).containsKey(JsonLdConsts.ID)){
            NexusInstanceReference reference = NexusInstanceReference.createFromUrl((String) ((Map) releaseInstance).get(JsonLdConsts.ID));
            Number revision = (Number)this.spec.getQualifiedMap().get(HBPVocabulary.RELEASE_REVISION);
            if(revision!=null && reference!=null) {
                reference.setRevision(revision.intValue());
            }
            return reference;
        }
        return null;
    }


}
