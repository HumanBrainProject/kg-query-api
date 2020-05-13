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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;

import java.util.ArrayList;
import java.util.List;

@ToBeTested(easy = true)
public class Vertex implements VertexOrEdge {

    private final List<Edge> edges;

    private final QualifiedIndexingMessage qualifiedIndexingMessage;

    private NexusInstanceReference instanceReference;

    public Vertex(QualifiedIndexingMessage qualifiedIndexingMessage) {
        this.qualifiedIndexingMessage = qualifiedIndexingMessage;
        this.instanceReference = qualifiedIndexingMessage.getOriginalMessage().getInstanceReference();
        this.edges = new ArrayList<>();
    }

    public void toSubSpace(SubSpace subSpace){
        this.instanceReference = this.instanceReference.toSubSpace(subSpace);
    }

    public void setInstanceReference(NexusInstanceReference instanceReference) {
        this.instanceReference = instanceReference;
    }

    public NexusInstanceReference getInstanceReference() {
        return instanceReference;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public QualifiedIndexingMessage getQualifiedIndexingMessage() {
        return qualifiedIndexingMessage;
    }
}
