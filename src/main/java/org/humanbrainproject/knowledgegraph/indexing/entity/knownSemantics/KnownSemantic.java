/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.indexing.entity.knownSemantics;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToBeTested(easy = true)
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
