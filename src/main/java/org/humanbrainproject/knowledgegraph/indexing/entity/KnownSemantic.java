package org.humanbrainproject.knowledgegraph.indexing.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class KnownSemantic {

    protected final QualifiedIndexingMessage spec;
    private final String type;

    public KnownSemantic(QualifiedIndexingMessage spec, String type) {
        this.spec = spec;
        this.type = type;
    }

    public boolean isInstance() {
        return spec.isOfType(type);
    }

    protected NexusInstanceReference getReferenceForLinkedInstance(Object linkedInstance, boolean onlyHttp) {
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
        return result != null ? NexusInstanceReference.createFromUrl(result) : null;
    }

    protected List<NexusInstanceReference> getReferencesForLinkedInstances(String key, boolean onlyHttp){
        if(spec.getQualifiedMap().containsKey(key)) {
            Object releaseInstance = spec.getQualifiedMap().get(key);
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

    protected List<NexusInstanceReference> getReferencesForLinkedInstances(Object element, boolean onlyHttp) {
        if (element instanceof List) {
            List<Object> releaseInstances = ((List<Object>) element);
            return releaseInstances.stream().map(i -> getReferenceForLinkedInstance(i, onlyHttp)).collect(Collectors.toList());
        } else {
            return Collections.singletonList(getReferenceForLinkedInstance(element, onlyHttp));
        }
    }

}
