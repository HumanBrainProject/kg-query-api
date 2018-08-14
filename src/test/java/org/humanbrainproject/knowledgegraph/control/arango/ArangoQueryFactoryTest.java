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
        collectionNames.add("foo-org-bar-v0_0_1");
        collectionNames.add("bar-org-foo-v0_0_1");
        String query = repository.createEmbeddedInstancesQuery(collectionNames, "helloWorld", null);
        assertEquals("FOR v, e IN 1..1 OUTBOUND \"helloWorld\" `bar-org-foo-v0_0_1`, `foo-org-bar-v0_0_1` \n" +
                "        \n" +
                "        return {\"vertexId\":v._id, \"edgeId\": e._id, \"isEmbedded\": v.`http://schema.hbp.eu/internal#embedded`==true}", query);
    }

    @Test
    public void createQueryEdgesToBeRemoved(){
        Set<String> collectionNames = new HashSet<String>();
        collectionNames.add("foo-org-bar-v0_0_1");
        collectionNames.add("bar-org-foo-v0_0_1");
        Set<String> excludeIds = new HashSet<>();
        excludeIds.add("excludeA");
        excludeIds.add("excludeB");

        String query = repository.queryEdgesToBeRemoved("helloWorld", collectionNames, excludeIds, null);
        assertEquals("LET doc = DOCUMENT(\"helloWorld\")\n" +
                "    FOR v, e IN OUTBOUND doc `bar-org-foo-v0_0_1`, `foo-org-bar-v0_0_1`\n" +
                "       FILTER e._id NOT IN [\"excludeB\", \"excludeA\"]\n" +
                "       return e._id", query);
    }
}