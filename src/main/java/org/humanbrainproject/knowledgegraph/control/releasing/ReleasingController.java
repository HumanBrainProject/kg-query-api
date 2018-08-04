package org.humanbrainproject.knowledgegraph.control.releasing;

import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ReleasingController {

    private static String RELEASE_TYPE="http://hbp.eu/minds#Release";

    public boolean isRelevantForReleasing(List<JsonLdVertex> vertices){
        for (JsonLdVertex vertex : vertices) {
            JsonLdProperty typeProperty = vertex.getTypeProperty();
            if(RELEASE_TYPE.equals(typeProperty.getValue())){
                return true;
            }
        }
        return false;
    }


    public List<JsonLdVertex> getVerticesToBeReleased(List<JsonLdVertex> originalVertices){
        return originalVertices;
    }

}
