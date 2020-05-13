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