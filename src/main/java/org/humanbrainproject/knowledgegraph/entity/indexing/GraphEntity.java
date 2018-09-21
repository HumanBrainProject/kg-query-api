package org.humanbrainproject.knowledgegraph.entity.indexing;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphEntity {

    protected final QualifiedGraphIndexingSpec spec;
    private final String type;

    public GraphEntity(QualifiedGraphIndexingSpec spec, String type) {
        this.spec = spec;
        this.type = type;
    }

    public boolean isInstance() {
        return spec.getMap().containsKey(JsonLdConsts.TYPE) && this.type.equals(spec.getMap().get(JsonLdConsts.TYPE));
    }


    public List<JsonLdVertex> getVertices(){
        return spec.getVertices();
    }

    protected String getReferenceForLinkedInstance(Object linkedInstance, boolean onlyHttp) {
        String result = null;
        if (linkedInstance instanceof Map && ((Map) linkedInstance).containsKey(JsonLdConsts.ID)) {
            Object resultObject = ((Map) linkedInstance).get(JsonLdConsts.ID);
            if(resultObject!=null){
                result = resultObject.toString();
            }
        }
        else if(linkedInstance!=null){
            result = linkedInstance.toString();
        }
        if(result!=null && onlyHttp && !result.startsWith("http")){
            result = null;
        }
        return result;
    }

    protected List<String> getReferencesForLinkedInstances(Map map, String key, boolean onlyHttp){
        if(map.containsKey(key)) {
            Object releaseInstance = map.get(key);
            return getReferencesForLinkedInstances(releaseInstance, onlyHttp);
        }
        else{
            return Collections.emptyList();
        }
    }

    protected List getValueListForProperty(Map map, String key){
        if(map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof List) {
                return (List) value;
            } else if(value!=null) {
                return Collections.singletonList(value);
            } else{
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    protected List<String> getReferencesForLinkedInstances(Object element, boolean onlyHttp) {
        if (element instanceof List) {
            List<Object> releaseInstances = ((List<Object>) element);
            return releaseInstances.stream().map(i -> getReferenceForLinkedInstance(i, onlyHttp)).collect(Collectors.toList());
        } else {
            return Collections.singletonList(getReferenceForLinkedInstance(element, onlyHttp));
        }
    }

    @Override
    public String toString() {
        return type+" {" +spec.getSpec().getEntityName()+" "+spec.getSpec().getId()+"}";
    }
}
