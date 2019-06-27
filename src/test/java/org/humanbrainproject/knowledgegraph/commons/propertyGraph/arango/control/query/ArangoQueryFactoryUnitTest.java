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