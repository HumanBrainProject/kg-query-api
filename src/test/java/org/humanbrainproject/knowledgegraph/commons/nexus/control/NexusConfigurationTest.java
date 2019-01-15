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
        String nexusBase = this.nexusConfiguration.getNexusBase(NexusConfiguration.ResourceType.DATA);
        assertEquals("http://foo/v0/data", nexusBase);
    }

    @Test
    public void getEndpointForResourceType() {
        String nexusEndpoint = this.nexusConfiguration.getEndpoint(NexusConfiguration.ResourceType.DATA);
        assertEquals("http://bar/v0/data", nexusEndpoint);
    }

    @Test
    public void getEndpointByRelativeUrl() {
        NexusRelativeUrl relativeUrl = new NexusRelativeUrl(NexusConfiguration.ResourceType.DATA, "foobar");
        String endpoint = this.nexusConfiguration.getEndpoint(relativeUrl);
        assertEquals("http://bar/v0/data/foobar", endpoint);
    }

    @Test
    public void getAbsoluteUrlForSchemaReference() {
        NexusSchemaReference schemaReference = new NexusSchemaReference("foo", "core", "bar", "v1.0.0");
        String absoluteUrl = this.nexusConfiguration.getAbsoluteUrl(schemaReference);
        assertEquals("http://foo/v0/schemas/foo/core/bar/v1.0.0", absoluteUrl);
    }

    @Test
    public void getAbsoluteUrlForInstanceReference() {
        NexusInstanceReference instanceReference = new NexusInstanceReference(new NexusSchemaReference("foo", "core", "bar", "v1.0.0"), "fooBar");
        String absoluteUrl = this.nexusConfiguration.getAbsoluteUrl(instanceReference);
        assertEquals("http://foo/v0/data/foo/core/bar/v1.0.0/fooBar", absoluteUrl);
    }
}