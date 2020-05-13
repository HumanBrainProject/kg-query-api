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

package org.humanbrainproject.knowledgegraph.converters.control;

import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class Shacl2EditorTest {

    private JsonLdStandardization jsonLdStandardization;

    private Shacl2Editor shacl2Editor;


    @Before
    public void setup(){
        jsonLdStandardization = new JsonLdStandardization();
        shacl2Editor = new Shacl2Editor();
    }


    private JsonDocument loadShaclSchema(String file){
      try {
          String s = IOUtils.toString(this.getClass().getResourceAsStream("/converters/control/shacl/" + file), "UTF-8");
          Map map = new JsonTransformer().parseToMap(s);
          return new JsonDocument(jsonLdStandardization.fullyQualify(map));
      }
      catch (IOException e){
          e.printStackTrace();
          return null;
      }
    }


//    @Test
//    public void convert() {
//        JsonDocument jsonDocument = loadShaclSchema("datashapes_morphology_invitroslicereconstructedneuronmorphology.json");
//
//        JsonDocument editor = shacl2Editor.convert(new NexusSchemaReference("foo", "bar", "foobar", "v1.0.0"), Collections.singletonList(jsonDocument));
//
//        System.out.println(new JsonTransformer().getMapAsJson(editor));
//    }
}