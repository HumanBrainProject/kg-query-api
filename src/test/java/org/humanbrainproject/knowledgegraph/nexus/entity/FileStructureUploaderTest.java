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
