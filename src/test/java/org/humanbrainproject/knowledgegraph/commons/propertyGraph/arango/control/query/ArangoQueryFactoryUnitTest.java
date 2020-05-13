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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;

public class ArangoQueryFactoryUnitTest {

    @Test
    public void testListInstancesByReference(){
        ArangoQueryFactory factory = new ArangoQueryFactory();

        ArangoCollectionReference collA = new ArangoCollectionReference("minds-core-dataset-v1_0_0");
        ArangoCollectionReference collB = new ArangoCollectionReference("minds-core-person-v1_0_0");


        ArangoDocumentReference docA1 = new ArangoDocumentReference(collA, "3e8004c8-f599-49e5-aa3c-98a054acc1a0");
        ArangoDocumentReference docA2 = new ArangoDocumentReference(collA, "bd6341d9-a5cc-4257-9cc9-0ecd376051af");

        ArangoDocumentReference docB1 = new ArangoDocumentReference(collB, "8ce641e3-39d6-4c75-a109-4f766e2c712a");

        String query = factory.listInstancesByReferences(new HashSet<>(Arrays.asList(docA1, docA2, docB1)), Collections.singleton("minds"));

        System.out.println(query);


    }


}