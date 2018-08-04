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
        v.setEntityName("http://nexus.humanbrainproject.org/v0/schema/org/domain/schema/v0.0.4");
        String outcome = namingConvention.getVertexLabel(v.getEntityName());
        assertEquals("org-domain-schema-v0_0_4", outcome);
    }


    @Test
    public void getKeyFromReference(){
        String outcome = namingConvention.getKeyFromReference("http://nexus.humanbrainproject.org/v0/schema/org/domain/schema/v0.0.4/id/dfs", false);
        assertEquals("org-domain-schema-v0_0_4/id-dfs", outcome);
    }


    @Test
    public void getKeyFromExternalReference(){
        String outcome = namingConvention.getKeyFromReference("http://foo.com/somereference", false);
        assertEquals("http://foo.com/somereference", outcome);
    }

    @Test
    public void getKeyFromEmbeddedInstance(){
        String outcome = namingConvention.getKeyFromReference("minds/ethics/approval/v0.0.4/8542d679-9e15-471d-97e2-cbe915163a18@http://www.w3.org/ns/prov#qualifiedAssociation", true);
        assertEquals("www_w3_org-ns-prov-qualifiedAssociation/xxxx", outcome);
    }


//    @Test
//    public void reduceLengthOfStringTooLong(){
//        String tooLong = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
//        String result = namingConvention.reduceLengthOfCharacters(tooLong);
//        assertEquals("stuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz", result);
//    }
//
//    @Test
//    public void reduceLengthOfStringExact(){
//        String tooLong = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefgh";
//        String result = namingConvention.reduceLengthOfCharacters(tooLong);
//        assertEquals("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefgh", result);
//    }
}