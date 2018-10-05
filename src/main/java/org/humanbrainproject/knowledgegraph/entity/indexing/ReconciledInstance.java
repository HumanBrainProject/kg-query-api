package org.humanbrainproject.knowledgegraph.entity.indexing;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ReconciledInstance {

    private final Set<String> instanceIds;

    private Map<String, Map> resolvedInstancesByInstanceId;

    private Map finalMap;

    public void setResolvedInstancesByInstanceId(Map<String, Map> resolvedInstancesByInstanceId) {
        this.resolvedInstancesByInstanceId = resolvedInstancesByInstanceId;
    }

    public ReconciledInstance(Set<String> instanceIds) {
        this.instanceIds = Collections.unmodifiableSet(instanceIds);
    }

    private Set<VirtualLink> virtualLinks;

    public Set<VirtualLink> getVirtualLinks() {
        return virtualLinks;
    }

    public void setVirtualLinks(Set<VirtualLink> virtualLinks) {
        this.virtualLinks = virtualLinks;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public Map<String, Map> getResolvedInstancesByInstanceId() {
        return resolvedInstancesByInstanceId;
    }

    public Map getFinalMap() {
        return finalMap;
    }

    public void setFinalMap(Map finalMap) {
        this.finalMap = finalMap;
    }
}
