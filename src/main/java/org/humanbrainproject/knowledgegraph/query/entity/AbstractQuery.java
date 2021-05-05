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

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tested
public abstract class AbstractQuery {

    private final NexusSchemaReference schemaReference;
    private Filter filter = new Filter();
    private Pagination pagination = new Pagination();
    private final String vocabulary;
    private  Map<String, String> parameters = new HashMap<>();

    public AbstractQuery(NexusSchemaReference schemaReference, String vocabulary) {
        this.schemaReference = schemaReference;
        this.vocabulary = vocabulary;
    }

    protected AbstractQuery(NexusSchemaReference schemaReference, String vocabulary, Filter filter, Pagination pagination, Map<String, String> parameters){
        this.schemaReference = schemaReference;
        this.vocabulary = vocabulary;
        this.filter = filter;
        this.pagination = pagination;
        this.parameters = parameters;
    }

    public NexusSchemaReference getSchemaReference() {
        return schemaReference;
    }

    public String getVocabulary() {
        return vocabulary;
    }

    public Filter getFilter() {
        return filter;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public Set<NexusInstanceReference> getInstanceReferencesWhitelist(){
        return getFilter().getRestrictToIds()!=null ? getFilter().getRestrictToIds().stream().map(id -> new NexusInstanceReference(schemaReference, id)).collect(Collectors.toSet()) : null;
    }

    public Set<ArangoDocumentReference> getDocumentReferenceWhitelist(){
        Set<NexusInstanceReference> instanceReferencesWhitelist = getInstanceReferencesWhitelist();
        return instanceReferencesWhitelist!=null ? instanceReferencesWhitelist.stream().map(ArangoDocumentReference::fromNexusInstance).collect(Collectors.toSet()) : null;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> p){
        if(p != null){
            this.parameters = p;
        }
    }
}
