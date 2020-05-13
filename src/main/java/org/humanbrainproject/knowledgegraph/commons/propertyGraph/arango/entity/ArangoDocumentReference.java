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
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

import java.util.Objects;

@ToBeTested(easy = true)
public class ArangoDocumentReference {

    private final ArangoCollectionReference collection;
    private final String key;

    public ArangoDocumentReference(ArangoCollectionReference collection, String key) {
        this.key = key;
        this.collection = collection;
    }

    public static ArangoDocumentReference fromEdge(Edge edge){
        ArangoCollectionReference collection = ArangoCollectionReference.fromEdge(edge);
        return new ArangoDocumentReference(collection, ArangoNamingHelper.createCompatibleId(edge.getId()));
    }

    public static ArangoDocumentReference fromId(String id) {
        String[] split = id.split("/");
        if (split.length == 2) {
            return new ArangoDocumentReference(new ArangoCollectionReference(split[0]), split[1]);
        }
        return new ArangoDocumentReference(new ArangoCollectionReference("unknown"), id);
    }

    public static ArangoDocumentReference fromNexusInstance(NexusInstanceReference path) {
        return fromNexusInstance(path, null);

    }

    public static ArangoDocumentReference fromNexusInstance(NexusInstanceReference path, SubSpace subSpace) {
        NexusSchemaReference nexusSchema = path.getNexusSchema();
        if (subSpace != null) {
            nexusSchema = nexusSchema.toSubSpace(subSpace);
        }
        ArangoCollectionReference collection = ArangoCollectionReference.fromNexusSchemaReference(nexusSchema);
        ArangoToNexusLookupMap.addToSchemaReferenceMap(collection, nexusSchema);
        return new ArangoDocumentReference(collection, path.getId());
    }

    public String getKey() {
        return key;
    }

    public ArangoCollectionReference getCollection() {
        return collection;
    }


    public String getId() {
        return String.format("%s/%s", getCollection().getName(), getKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoDocumentReference that = (ArangoDocumentReference) o;
        return Objects.equals(collection, that.collection) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, key);
    }
}
