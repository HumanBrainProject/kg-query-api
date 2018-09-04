package org.humanbrainproject.knowledgegraph.control.json;

import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class JsonTransformerTest {

    @Test
    public void resolveNestedProperty() throws JSONException {
        JsonLdProperty property = new JsonLdProperty();
        property.setName("foo");
        JsonLdProperty property2  = new JsonLdProperty();
        property2.setName("bar");
        property2.setValue("foobar");
        property.setValue(property2);
        JsonTransformer jsonTransformer = new JsonTransformer();
        Object result = jsonTransformer.resolveProperty(property);
        Assert.assertEquals("{\"foo\":{\"bar\":\"foobar\"}}", result.toString());
    }


    @Test
    public void resolveSimpleProperty() throws JSONException {
        JsonLdProperty property = new JsonLdProperty();
        property.setName("foo");
        property.setValue("bar");
        JsonTransformer jsonTransformer = new JsonTransformer();
        Object result = jsonTransformer.resolveProperty(property);
        Assert.assertEquals("{\"foo\":\"bar\"}", result.toString());
    }

    @Test
    public void resolveSimpleInteger() throws JSONException {
        JsonLdProperty property = new JsonLdProperty();
        property.setName("foo");
        property.setValue(3);
        JsonTransformer jsonTransformer = new JsonTransformer();
        Object result = jsonTransformer.resolveProperty(property);
        Assert.assertEquals("{\"foo\":3}", result.toString());
    }

    @Test
    public void resolveCollectionOfProperties() throws JSONException {
        JsonLdProperty property = new JsonLdProperty();
        property.setName("foo");
        Set<JsonLdProperty> properties = new LinkedHashSet<>();
        JsonLdProperty property2  = new JsonLdProperty();
        property2.setName("bar");
        property2.setValue("foobar");
        properties.add(property2);
        property.setValue(properties);
        JsonTransformer jsonTransformer = new JsonTransformer();
        Object result = jsonTransformer.resolveProperty(property);
        Assert.assertEquals("{\"foo\":[{\"bar\":\"foobar\"}]}", result.toString());
    }


    @Test
    public void recreateObjectFromProperties() throws JSONException {
        Set<JsonLdProperty> properties = new LinkedHashSet<>();
        JsonLdProperty property = new JsonLdProperty();
        property.setName("foo");
        JsonLdProperty property2  = new JsonLdProperty();
        property2.setName("bar");
        property2.setValue("foobar");
        property.setValue(property2);

        JsonLdProperty property3 = new JsonLdProperty();
        property3.setName("foobar");
        property3.setValue("barfoo");

        properties.add(property3);
        properties.add(property);

        JsonTransformer jsonTransformer = new JsonTransformer();

        JSONObject jsonObject = jsonTransformer.recreateObjectFromProperties(properties);

        Assert.assertEquals("{\"foobar\":\"barfoo\",\"foo\":{\"bar\":\"foobar\"}}", jsonObject.toString());
    }
}