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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph;

import org.humanbrainproject.knowledgegraph.annotations.NoTests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This is a marker interface to declare explicitly if a method requires authorization. There is no logic yet to validate if it actually does it yet.
 */

@NoTests(NoTests.NO_LOGIC)
@Target(ElementType.METHOD)
public @interface AuthorizedAccess {

    String value() default "";

}

