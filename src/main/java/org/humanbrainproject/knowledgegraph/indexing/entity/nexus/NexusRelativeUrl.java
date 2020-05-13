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

package org.humanbrainproject.knowledgegraph.indexing.entity.nexus;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tested
public class NexusRelativeUrl {
    private final String url;
    private final NexusConfiguration.ResourceType resourceType;
    Map<String, Object> parameters = new HashMap<>();

    public void addQueryParameter(String key, Object value){
        this.parameters.put(key, value);
    }

    public NexusRelativeUrl(NexusConfiguration.ResourceType resourceType, String relativeUrl) {
        this.url = relativeUrl;
        this.resourceType = resourceType;
    }



    public NexusConfiguration.ResourceType getResourceType() {
        return resourceType;
    }

    public String getUrl() {
        if(url==null && parameters.isEmpty()){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        if(parameters.size()>0){
            sb.append("?");
        }
        for (String s : parameters.keySet()) {
            sb.append('&').append(s).append('=').append(parameters.get(s));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NexusRelativeUrl that = (NexusRelativeUrl) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
