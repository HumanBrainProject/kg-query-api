package org.humanbrainproject.knowledgegraph.commons.nexus.control;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NexusConfigurationTest {

    NexusConfiguration nexusConfiguration;

    @Before
    public void setUp() {
        this.nexusConfiguration = TestObjectFactory.createNexusConfiguration();
    }

    @Test
    public void getNexusBaseForResourceType() {
        String nexusBase = this.nexusConfiguration.getNexusBase(NexusConfiguration.ResourceType.RESOURCES);
        assertEquals("http://foo/resources", nexusBase);
    }

    @Test
    public void getEndpointForResourceType() {
        String nexusEndpoint = this.nexusConfiguration.getNexusEndpoint(NexusConfiguration.ResourceType.RESOURCES);
        assertEquals("http://bar/resources", nexusEndpoint);
    }

    @Test
    public void getEndpointByRelativeUrl() {
        NexusRelativeUrl relativeUrl = new NexusRelativeUrl(NexusConfiguration.ResourceType.RESOURCES, "foobar");
        String endpoint = this.nexusConfiguration.getNexusEndpoint(relativeUrl);
        assertEquals("http://bar/resources/foobar", endpoint);
    }

    @Test
    public void getAbsoluteUrlForSchemaReference() {
        NexusSchemaReference schemaReference = new NexusSchemaReference("foo", "core", "bar", "v1.0.0");
        String absoluteUrl = this.nexusConfiguration.getAbsoluteUrl(schemaReference);
        assertEquals("http://bar/schemas/foo/core/bar/v1.0.0", absoluteUrl);
    }

    @Test
    public void getAbsoluteUrlForInstanceReference() {
        NexusInstanceReference instanceReference = new NexusInstanceReference(new NexusSchemaReference("foo", "core", "bar", "v1.0.0"), "fooBar");
        String absoluteUrl = this.nexusConfiguration.getAbsoluteUrl(instanceReference);
        assertEquals("http://bar/resources/foo/core/bar/v1.0.0/fooBar", absoluteUrl);
    }
}