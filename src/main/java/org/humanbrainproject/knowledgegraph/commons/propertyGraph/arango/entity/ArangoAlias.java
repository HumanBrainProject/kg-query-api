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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;

import java.util.Objects;

@ToBeTested(easy = true)
public class ArangoAlias {
    private final String aliasDocPostfix = "_doc";


    private final String originalName;

    public ArangoAlias(String name) {
        this.originalName = name;
    }

    public static ArangoAlias fromOriginalFieldName(String fieldName){
        return new ArangoAlias(fieldName);
    }

    public static ArangoAlias fromSpecField(SpecField specField){
        return new ArangoAlias(specField.fieldName);
    }

    public ArangoAlias increment(){
        return new ArangoAlias(this.originalName+"_");
    }

    public static ArangoAlias fromLeafPath(SpecTraverse specTraverse){
        return new ArangoAlias(specTraverse.pathName);
    }

    public String getArangoDocName(){
        return getArangoName()+aliasDocPostfix;
    }

    public String getArangoName() {
        return ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(originalName).replaceAll("-", "_"));
    }

    public String getOriginalName() {
        return originalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoAlias that = (ArangoAlias) o;
        return Objects.equals(originalName, that.originalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalName);
    }
}
