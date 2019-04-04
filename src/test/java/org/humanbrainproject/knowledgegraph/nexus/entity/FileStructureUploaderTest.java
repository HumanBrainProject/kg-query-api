package org.humanbrainproject.knowledgegraph.nexus.entity;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileStructureUploaderTest {

    @Test
    public void retry(){
        NexusDataStructure data = new NexusDataStructure();
        FileStructureUploader fu = new FileStructureUploader(data, null,null,null,null, false, false);
        List<String> s = new ArrayList<>();
        s.add("Test");
        String result = "FailedTest";
        FileStructureUploader.CheckedFunction<List<String>, List<String>> function = (List<String> i, int o) -> {
            return i.stream().map(myString -> result).collect(Collectors.toList());
        };
        try {
            List<String> ss = fu.withRetry(4, s, function, false);
            assert ss.size() == 1;
            assert ss.get(0).equals(result);
        } catch (IOException e){
            assert false;
        } catch (InterruptedException e){
            assert false;
        }
    }
}
