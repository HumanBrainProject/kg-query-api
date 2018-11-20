package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArangoRepositoryTest {

    @Test
    public void findWorstReleaseStatus() {

        Map<String, Object> map = new HashMap<>();
        map.put("status", ReleaseStatus.HAS_CHANGED.name());
        List<Object> children = new ArrayList<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("status", ReleaseStatus.RELEASED.name());
        children.add(childMap);
        map.put("children", children);

        ReleaseStatus worstReleaseStatus = new ArangoRepository().findWorstReleaseStatusOfChildren(map, null, true);

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

        ReleaseStatus worstReleaseStatus = new ArangoRepository().findWorstReleaseStatusOfChildren(map, null, true);

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

        ReleaseStatus worstReleaseStatus = new ArangoRepository().findWorstReleaseStatusOfChildren(map, null, true);

        Assert.assertEquals(ReleaseStatus.NOT_RELEASED, worstReleaseStatus);
    }
}