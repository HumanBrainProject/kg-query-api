package org.humanbrainproject.knowledgegraph.commons.labels;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.junit.Assert;
import org.junit.Test;

public class SemanticsToHumanTranslatorTest {

    @Test
    public void translateTypeWithHashToHumanReadableLabel() {
        String humanReadable = new SemanticsToHumanTranslator().translateSemanticValueToHumanReadableLabel("http://hbp.eu/minds#Dataset");

        Assert.assertEquals("Dataset", humanReadable);
    }

    @Test
    public void translateTypeWithSlashToHumanReadableLabel() {
        String humanReadable = new SemanticsToHumanTranslator().translateSemanticValueToHumanReadableLabel("http://schema.hbp.eu/minds/Dataset");

        Assert.assertEquals("Dataset", humanReadable);
    }

    @Test
    public void translatePropertyToHumanReadableLabel() {
        String humanReadable = new SemanticsToHumanTranslator().translateSemanticValueToHumanReadableLabel("https://schema.hbp.eu/provenance/indexedInArangoAt");
        Assert.assertEquals("indexed In Arango At", humanReadable);
    }

    @Test
    public void translateArangoCollectionName(){
        String humanReadable = new SemanticsToHumanTranslator().translateArangoCollectionName(new ArangoCollectionReference("www_w3_org-ns-prov-agent"));
        Assert.assertEquals("Agent", humanReadable);

    }
}