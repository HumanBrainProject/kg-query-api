/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoTests(NoTests.TRIVIAL)
public class Filter {

    private List<String> restrictToOrganizations;
    private List<String> restrictToIds;

    private String queryString;

    public Filter setQueryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    public String getQueryString() {
        return queryString;
    }

    public Filter restrictToOrganizations(String[] whitelistOfOrganizations) {
        this.restrictToOrganizations = whitelistOfOrganizations == null ? null : Collections.unmodifiableList(Arrays.stream(whitelistOfOrganizations).map(String::trim).collect(Collectors.toList()));

        return this;
    }

    public Filter restrictToIds(String[] ids) {
        this.restrictToIds = ids == null ? null : Collections.unmodifiableList(Arrays.stream(ids).map(String::trim).collect(Collectors.toList()));
        return this;
    }

    public Filter restrictToSingleId(String id) {
        this.restrictToIds = id == null ? null : Collections.singletonList(id.trim());
        return this;
    }

    public List<String> getRestrictToOrganizations() {
        return restrictToOrganizations;
    }

    public List<String> getRestrictToIds() {
        return restrictToIds;
    }



}
