package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.junit.Test;

import static org.junit.Assert.*;

public class TemplateTest {

    @Test
    public void asJsonDocument() {

        Template template = new Template();
        template.key="foo/bar";
        template.templateContent = "fooTemplateContent";
        template.library = "fooLibrary";

        JsonDocument jsonDocument = template.asJsonDocument();

        assertEquals("foo-bar", jsonDocument.get(ArangoVocabulary.KEY));
        assertEquals("fooTemplateContent", jsonDocument.get("templateContent"));
        assertEquals("fooLibrary", jsonDocument.get("library"));


    }
}