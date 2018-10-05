package org.humanbrainproject.knowledgegraph.entity.indexing;

import java.util.List;

public class OriginalSpace extends GraphEntity{

    private static final String RELEASE_TYPE = "http://hbp.eu/minds#Release";
    private static final String RELEASE_INSTANCE_PROPERTYNAME = "http://hbp.eu/minds#releaseinstance";

    public OriginalSpace(QualifiedGraphIndexingSpec spec) {
        super(spec, RELEASE_TYPE);
    }

    public List<String> getReleaseInstances(){
        return getReferencesForLinkedInstances(spec.getMap(), RELEASE_INSTANCE_PROPERTYNAME, true);
    }


}
