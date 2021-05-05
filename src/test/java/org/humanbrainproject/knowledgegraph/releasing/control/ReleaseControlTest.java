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

package org.humanbrainproject.knowledgegraph.releasing.control;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReleaseControlTest {

    @Test
    public void findWorstReleaseStatus() {

        Map<String, Object> map = new HashMap<>();
        map.put("status", ReleaseStatus.HAS_CHANGED.name());
        List<Object> children = new ArrayList<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("status", ReleaseStatus.RELEASED.name());
        children.add(childMap);
        map.put("children", children);

        ReleaseStatus worstReleaseStatus = new ReleaseControl().findWorstReleaseStatusOfChildren(map, null, true);

        //The worst release state is RELEASED, because root is not taken into account
        Assert.assertEquals(ReleaseStatus.RELEASED, worstReleaseStatus);
    }

    @Test
    public void findWorstReleaseStatus2() {

        Map<String, Object> map = new HashMap<>();
        map.put("status", ReleaseStatus.HAS_CHANGED.name());
        List<Object> children = new ArrayList<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("status", ReleaseStatus.RELEASED.name());
        children.add(childMap);
        Map<String, Object> childMap2 = new HashMap<>();
        childMap2.put("status", ReleaseStatus.HAS_CHANGED.name());
        children.add(childMap2);
        map.put("children", children);

        ReleaseStatus worstReleaseStatus = new ReleaseControl().findWorstReleaseStatusOfChildren(map, null, true);

        //The worst release state is RELEASED, because root is not taken into account
        Assert.assertEquals(ReleaseStatus.HAS_CHANGED, worstReleaseStatus);
    }

    @Test
    public void findWorstReleaseStatus3() {

        Map<String, Object> map = new HashMap<>();
        map.put("status", ReleaseStatus.HAS_CHANGED.name());
        List<Object> children = new ArrayList<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("status", ReleaseStatus.NOT_RELEASED.name());
        children.add(childMap);
        map.put("children", children);

        ReleaseStatus worstReleaseStatus = new ReleaseControl().findWorstReleaseStatusOfChildren(map, null, true);

        Assert.assertEquals(ReleaseStatus.NOT_RELEASED, worstReleaseStatus);
    }
}