package org.humanbrainproject.knowledgegraph.control.arango;

import com.google.gson.Gson;
import org.junit.Test;

import java.util.Map;

public class GsonTest {

    Gson gson = new Gson();

    @Test
    public void testSingleInstanceList(){
        String json = "{\"foo\": [{\"bar\":\"foobar\"}]}";
        Map map = gson.fromJson(json, Map.class);

        System.out.println(map);

    }


}
