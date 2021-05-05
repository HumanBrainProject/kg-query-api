/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

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