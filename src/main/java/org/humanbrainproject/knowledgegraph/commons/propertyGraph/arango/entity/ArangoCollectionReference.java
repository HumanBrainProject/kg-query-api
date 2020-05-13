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
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoToNexusLookupMap;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;

import java.util.Objects;

@ToBeTested(easy = true)
public class ArangoCollectionReference {


    private final String name;

    public ArangoCollectionReference(String collectionName) {
        this.name = collectionName;
    }

    public static ArangoCollectionReference fromFieldName(String fieldName){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(ArangoNamingHelper.removeTrailingHttps(fieldName))));
    }

    public static ArangoCollectionReference fromNexusSchemaReference(NexusSchemaReference path){
        ArangoCollectionReference collectionReference = new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(path.getRelativeUrl().getUrl())));
        ArangoToNexusLookupMap.addToSchemaReferenceMap(collectionReference, path);
        return collectionReference;
    }

    public static ArangoCollectionReference fromEdge(Edge edge){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(ArangoNamingHelper.removeTrailingHttps(edge.getName()))));
    }

    public static ArangoCollectionReference fromSpecTraversal(SpecTraverse specTraverse){
        return new ArangoCollectionReference(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(ArangoNamingHelper.removeTrailingHttps(specTraverse.pathName))));
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoCollectionReference that = (ArangoCollectionReference) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
