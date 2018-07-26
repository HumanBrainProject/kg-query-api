package org.humanbrainproject.knowledgegraph.control.arango;

import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.junit.Test;

import static org.junit.Assert.*;

public class ArangoNamingConventionTest {

    ArangoNamingConvention namingConvention = new ArangoNamingConvention();


    @Test
    public void removeSpecialCharacters(){
        String s = namingConvention.replaceSpecialCharacters("http://foo.bar/hello_world-1234#uuu");
        assertEquals("foo_bar-hello_world-1234-uuu", s);
    }

    @Test
    public void getVertexLabel(){
        JsonLdVertex v = new JsonLdVertex();
        v.setType("http://nexus.humanbrainproject.org/v0/schema/org/domain/schema/v0.0.4");
        String outcome = namingConvention.getVertexLabel(v);
        assertEquals("org-domain-schema-v0_0_4", outcome);
    }


    @Test
    public void getKeyFromReference(){
        String outcome = namingConvention.getKeyFromReference("http://nexus.humanbrainproject.org/v0/schema/org/domain/schema/v0.0.4/id/dfs");
        assertEquals("org-domain-schema-v0_0_4/id-dfs", outcome);
    }


    @Test
    public void getKeyFromExternalReference(){
        String outcome = namingConvention.getKeyFromReference("http://foo.com/somereference");
        assertEquals("http://foo.com/somereference", outcome);
    }
}