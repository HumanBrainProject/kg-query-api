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
