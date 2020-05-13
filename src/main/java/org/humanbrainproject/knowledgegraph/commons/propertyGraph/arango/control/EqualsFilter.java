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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;

public class EqualsFilter {

    public final TrustedAqlValue key;
    public final String value;

    public EqualsFilter(TrustedAqlValue key, String value) {
        this.key = key;
        this.value = value;
    }

    public EqualsFilter(String key, String value) {
        this.key = AQL.preventAqlInjection(key);
        this.value = value;
    }

    public TrustedAqlValue getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
