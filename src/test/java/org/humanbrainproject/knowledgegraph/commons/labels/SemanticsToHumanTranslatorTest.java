package org.humanbrainproject.knowledgegraph.commons.labels;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SemanticsToHumanTranslatorTest {

    SemanticsToHumanTranslator translator;

    @Before
    public void setup(){
        this.translator = new SemanticsToHumanTranslator();
    }


    @Test
    public void translateTypeWithHashToHumanReadableLabel() {
        String humanReadable = translator.translateSemanticValueToHumanReadableLabel("https://schema.hbp.eu/minds/Dataset");
        assertEquals("Dataset", humanReadable);
    }

    @Test
    public void translateTypeWithSlashToHumanReadableLabel() {
        String humanReadable = translator.translateSemanticValueToHumanReadableLabel("https://schema.hbp.eu/minds/Dataset");

        assertEquals("Dataset", humanReadable);
    }

    @Test
    public void translatePropertyToHumanReadableLabel() {
        String humanReadable = translator.translateSemanticValueToHumanReadableLabel("https://schema.hbp.eu/provenance/indexedInArangoAt");
        assertEquals("Indexed in arango at", humanReadable);
    }

    @Test
    public void translateArangoCollectionName(){
        String humanReadable = translator.translateArangoCollectionName(new ArangoCollectionReference("www_w3_org-ns-prov-agent"));
        assertEquals("Agent", humanReadable);
    }


    @Test
    public void extractSimpleAttributeName(){
        String attributeName = translator.extractSimpleAttributeName("https://schema.hbp.eu/minds/Dataset");
        assertEquals("Dataset", attributeName);

    }

    @Test
    public void extractSimpleAttributeNameWithHash(){
        String attributeName = translator.extractSimpleAttributeName("https://schema.hbp.eu/minds/Dataset#foo");
        assertEquals("foo", attributeName);
    }

    @Test
    public void normalize(){
        String value = "@foo_barFooBar";
        String normalized = translator.normalize(value);
        assertEquals("Foo bar foo bar", normalized);
    }


}