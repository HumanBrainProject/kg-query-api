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

package org.humanbrainproject.knowledgegraph.releasing.entity;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseStatusTest {

    @Test
    public void isWorseThan() {
        boolean worseThan = ReleaseStatus.NOT_RELEASED.isWorseThan(ReleaseStatus.RELEASED);
        Assert.assertTrue(worseThan);
    }

    @Test
    public void isNotWorseThan() {
        boolean worseThan = ReleaseStatus.HAS_CHANGED.isWorseThan(ReleaseStatus.NOT_RELEASED);
        Assert.assertFalse(worseThan);
    }

    @Test
    public void isWorst() {
        Assert.assertTrue(ReleaseStatus.NOT_RELEASED.isWorst());
    }

    @Test
    public void isNotWorst() {
        Assert.assertFalse(ReleaseStatus.HAS_CHANGED.isWorst());
        Assert.assertFalse(ReleaseStatus.RELEASED.isWorst());
    }
}