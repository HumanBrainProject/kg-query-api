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

package org.humanbrainproject.knowledgegraph.query.entity;

import org.junit.Assert;
import org.junit.Test;

public class JsonDocumentTest {

    @Test
    public void removeAllInternalKeys() {
        JsonDocument document = new JsonDocument();
        document.addToProperty("_foo", "bar");
        document.addToProperty("bar", "foo");
        document.removeAllInternalKeys();

        Assert.assertEquals(1, document.size());
        Assert.assertEquals("foo", document.get("bar"));
    }
}