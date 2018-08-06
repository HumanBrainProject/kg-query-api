package org.humanbrainproject.knowledgegraph.control.arango;

import org.humanbrainproject.knowledgegraph.control.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ArangoQueryFactoryTest {

    ArangoQueryFactory repository;

    @Before
    public void setup(){
        repository = new ArangoQueryFactory();
        repository.configuration = new Configuration();
    }

    @Test
    public void createEmbeddedInstancesQuery() {
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.add("http://foo.org/bar");
        collectionNames.add("http://bar.org/foo");
        String query = repository.createEmbeddedInstancesQuery(collectionNames, "helloWorld");
        assertEquals("FOR v, e IN 1..1 OUTBOUND \"helloWorld\" `http://bar.org/foo`, `http://foo.org/bar` \n" +
                "        \n" +
                "        return {\"vertexId\":v._id, \"edgeId\": e._id, \"isEmbedded\": v.`http://schema.hbp.eu/internal#embedded`==true}", query);
    }
}