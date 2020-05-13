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

package org.humanbrainproject.knowledgegraph.releasing.boundary;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders.TreeScope;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.releasing.control.ReleaseControl;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class Releasing {

    @Autowired
    ReleaseControl releaseControl;

    public void release(NexusInstanceReference instanceReference) {
        releaseControl.release(instanceReference);
    }

    public NexusInstanceReference unrelease(NexusInstanceReference instanceReference) {
        return releaseControl.unrelease(instanceReference);
    }

    public ReleaseStatusResponse getReleaseStatus(NexusInstanceReference instanceReference, TreeScope scope) {
        return releaseControl.getReleaseStatus(instanceReference, scope);
    }

    public Map<String, Object> getReleaseGraph(NexusInstanceReference instanceReference) {
        return releaseControl.getReleaseGraph(instanceReference, TreeScope.ALL);
    }

}
