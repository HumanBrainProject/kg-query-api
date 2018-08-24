package org.humanbrainproject.knowledgegraph.control.template;

import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.control.Constants;
import org.humanbrainproject.knowledgegraph.control.jsonld.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FreemarkerMetaTemplatingTest {

    FreemarkerTemplating templating = new FreemarkerTemplating();
    JsonLdStandardization standardization = new JsonLdStandardization();
    Map specification;
    String freemarker;
    QueryResult<List<Map>> queryResult;
    @Before
    public void setup() throws IOException, JSONException {
        freemarker = IOUtils.toString(this.getClass().getResourceAsStream("/freemarker/meta.ftl"), "UTF-8");
        String json = IOUtils.toString(this.getClass().getResourceAsStream("/apiSpec/sample.json"), "UTF-8");
        Gson gson = new Gson();
        String spec = JsonUtils.toString(new JsonLdStandardization().fullyQualify(json));
        this.specification = gson.fromJson(spec, Map.class);
        queryResult = new QueryResult<>();
        queryResult.setResults(Collections.singletonList(specification));
        Map<String, String> context = new HashMap<>();
        context.put("spec", Constants.GRAPH_QUERY_VOCAB);
        queryResult.setResults(standardization.applyContext(queryResult.getResults(), context));
    }

    @Test
    public void applyMetaTemplate() {
        String result = templating.applyTemplate(freemarker, queryResult, null, Collections.emptyList());
        System.out.println(result);
    }
}