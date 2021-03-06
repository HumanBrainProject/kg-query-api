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

package org.humanbrainproject.knowledgegraph.commons.solr;

import org.apache.solr.client.solrj.SolrServerException;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

@Ignore("Integration test")
public class SolrTest {

    @Test
    public void registerCore() throws IOException, SolrServerException {

        Solr solr = new Solr();
        solr.solrBase = "http://localhost:8983/solr";
        solr.solrCore = "foo4";
        solr.removeCore();
        solr.registerCore();
        solr.registerPoints("bar", "foobar", new HashSet<>(Arrays.asList(new ThreeDVector(0.1, 0.2, 0.3), new ThreeDVector(0.2, 0.4, 0.6))));

    }
}