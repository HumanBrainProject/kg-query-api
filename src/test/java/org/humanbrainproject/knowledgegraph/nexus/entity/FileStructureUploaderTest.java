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

package org.humanbrainproject.knowledgegraph.nexus.entity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FileStructureUploaderTest {

    @Test
    public void retry(){
        NexusDataStructure data = new NexusDataStructure();
        FileStructureUploader fu = new FileStructureUploader(data, null,null, null,null,null, false, false);
        List<String> s = new ArrayList<>();
        s.add("Test");
        String result = "FailedTest";
        FileStructureUploader.CheckedFunction<List<String>, ErrorsAndSuccess<List<String>>> function = (List<String> i, ErrorsAndSuccess<List<String>> o) -> {
            o.errors = new ArrayList<>();
            o.errors.add(result);
            return o;
        };
        try {
            ErrorsAndSuccess<List<String>> ss = fu.withRetry(4, s, function, false);
            assert ss.errors.size() == 1;
            assert ss.errors.get(0).equals(result);
        } catch (InterruptedException e){
            assert false;
        }
    }
}
