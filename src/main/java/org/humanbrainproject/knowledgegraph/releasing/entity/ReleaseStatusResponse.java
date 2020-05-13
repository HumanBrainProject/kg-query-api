/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.releasing.entity;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.LinkedHashMap;

@Tested
public class ReleaseStatusResponse extends LinkedHashMap<String, String> {

    public void setId(NexusInstanceReference reference){
        this.put("id", reference!=null ? reference.getRelativeUrl().getUrl() : null);
    }

    public void setRootStatus(ReleaseStatus releaseStatus){
        if(releaseStatus==null){
            this.remove("status");
        }
        else {
            this.put("status", releaseStatus.name());
        }
    }

    public void setChildrenStatus(ReleaseStatus releaseStatus){
        if(releaseStatus==null){
            this.remove("childrenStatus");
        }
        else {
            this.put("childrenStatus", releaseStatus.name());
        }
    }

}
