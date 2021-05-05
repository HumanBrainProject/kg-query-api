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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArangoQueryFactoryTest {

    Set<String> permissionGroupsWithReadAccess = new HashSet<>(Arrays.asList("minds"));
    Set<ArangoCollectionReference> collections = Stream.of("schema_hbp_eu-minds-activity", "schema_hbp_eu-minds-specimen_group").map(ArangoCollectionReference::new).collect(Collectors.toSet());
    ArangoQueryFactory arangoQueryFactory;
    @Before
    public void setup(){
        arangoQueryFactory = new ArangoQueryFactory();
        arangoQueryFactory.configuration = Mockito.spy(new NexusConfiguration());
        Mockito.doReturn("https://nexus-dev.humanbrainproject.org").when(arangoQueryFactory.configuration).getNexusBase();
        Mockito.doReturn("https://nexus-dev.humanbrainproject.org").when(arangoQueryFactory.configuration).getNexusEndpoint();
    }


    @Test
    public void queryReleaseGraph() {
        ArangoDocumentReference documentReference = new ArangoDocumentReference(new ArangoCollectionReference("minds-core-dataset-v1_0_0"), "2da66d5d-54e1-4f8f-9caa-1b1ef020ef21");
        String releaseGraph = arangoQueryFactory.queryReleaseGraph(collections, documentReference, 1, permissionGroupsWithReadAccess);
        System.out.println(releaseGraph);
    }
}