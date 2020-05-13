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

import java.util.Arrays;
import java.util.Optional;

@Tested
public enum ReleaseStatus {
    RELEASED(1), NOT_RELEASED(3), HAS_CHANGED(2);

    int severity;

    ReleaseStatus(int severity){
        this.severity = severity;
    }

    public boolean isWorseThan(ReleaseStatus releaseStatus){
        return releaseStatus == null || this.severity > releaseStatus.severity;
    }

    public boolean isWorst(){
        Optional<Integer> max = Arrays.stream(ReleaseStatus.values()).map(s -> s.severity).reduce(Integer::max);
        return max.get()!=null && max.get()==this.severity;
    }

}
