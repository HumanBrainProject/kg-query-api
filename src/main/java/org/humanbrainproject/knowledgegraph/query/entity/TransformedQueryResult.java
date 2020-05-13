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

package org.humanbrainproject.knowledgegraph.query.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.List;
import java.util.Map;

@NoTests(NoTests.NO_LOGIC)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransformedQueryResult<T> extends QueryResult<T>{

    private List<Map> originalJson;

    public List<Map> getOriginalJson() {
        return originalJson;
    }

    public void setOriginalJson(List<Map> originalJson) {
        this.originalJson = originalJson;
    }

}
