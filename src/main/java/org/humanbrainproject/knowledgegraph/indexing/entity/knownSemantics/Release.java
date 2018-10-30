package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.KnownSemantic;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;

import java.util.Map;

public class Release extends KnownSemantic {

    public static final String RELEASE_TYPE = "http://hbp.eu/minds#Release";
    public static final String RELEASE_INSTANCE_PROPERTYNAME = "http://hbp.eu/minds#releaseinstance";
    public static final String RELEASE_REVISION_PROPERTYNAME = "http://hbp.eu/minds#releaserevision";

    public Release(QualifiedIndexingMessage spec) {
        super(spec, RELEASE_TYPE);
    }

    public NexusInstanceReference getReleaseInstance(){
        Object releaseInstance = this.spec.getQualifiedMap().get(RELEASE_INSTANCE_PROPERTYNAME);
        if(releaseInstance instanceof Map && ((Map)releaseInstance).containsKey(JsonLdConsts.ID)){
            NexusInstanceReference reference = NexusInstanceReference.createFromUrl((String) ((Map) releaseInstance).get(JsonLdConsts.ID));
            Number revision = (Number)this.spec.getQualifiedMap().get(RELEASE_REVISION_PROPERTYNAME);
            if(revision!=null && reference!=null) {
                reference.setRevision(revision.intValue());
            }
            return reference;
        }
        return null;
    }


}
