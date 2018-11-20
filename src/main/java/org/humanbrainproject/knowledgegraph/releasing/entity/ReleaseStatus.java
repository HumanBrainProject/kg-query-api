package org.humanbrainproject.knowledgegraph.releasing.entity;

import java.util.Arrays;
import java.util.Optional;

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
