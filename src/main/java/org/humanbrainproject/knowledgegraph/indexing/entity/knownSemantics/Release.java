package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.KnownSemantic;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;

import java.util.Map;

public class Release extends KnownSemantic {

    public static final String RELEASE_TYPE = "http://hbp.eu/minds#Release";
    private static final String RELEASE_INSTANCE_PROPERTYNAME = "http://hbp.eu/minds#releaseinstance";

    public Release(QualifiedIndexingMessage spec) {
        super(spec, RELEASE_TYPE);
    }

    public NexusInstanceReference getReleaseInstance(){
        Object releaseInstance = this.spec.getQualifiedMap().get(RELEASE_INSTANCE_PROPERTYNAME);
        if(releaseInstance instanceof Map && ((Map)releaseInstance).containsKey(JsonLdConsts.ID)){
            return NexusInstanceReference.createFromUrl((String)((Map)releaseInstance).get(JsonLdConsts.ID));
        }
        return null;
    }

}
