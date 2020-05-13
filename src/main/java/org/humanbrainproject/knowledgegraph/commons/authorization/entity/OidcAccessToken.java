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

package org.humanbrainproject.knowledgegraph.commons.authorization.entity;

import org.apache.commons.lang3.StringUtils;
import org.humanbrainproject.knowledgegraph.annotations.Tested;

import java.util.Objects;

@Tested
public class OidcAccessToken implements Credential {

    private String token;

    public OidcAccessToken setToken(String token) {
        this.token = token;
        return this;
    }

    public String getToken() {
        return token;
    }

    public String getBearerToken(){
        return this.token != null ? this.token.toLowerCase().startsWith("bearer ") ? StringUtils.capitalize(this.token) : String.format("Bearer %s", this.token) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OidcAccessToken that = (OidcAccessToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
