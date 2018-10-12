package org.humanbrainproject.knowledgegraph.control.arango.query;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.specification.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class ArangoQueryBuilderTest {
    Specification testSpecification;
    ArangoSpecificationQuery query;
    Set<String> whitelistedOrganizations;
    ArangoQueryBuilder queryBuilder;
    Integer size = 1;
    Integer start = 0;
    String search = "";
    String queryWithOrgHeader = "LET whitelist_organizations=[]\n";

    @Before
    public void setup() throws IOException, JSONException {
        String specification = IOUtils.toString(this.getClass().getResourceAsStream("/apiSpec/sample.json"), "UTF-8");
        String collectionLabels = IOUtils.toString(this.getClass().getResourceAsStream("/collectionLabels.json"), "UTF-8");
        Gson gson = new Gson();
        this.testSpecification = new SpecificationInterpreter().readSpecification(specification);
        query = new ArangoSpecificationQuery();
        query.arangoDriver = Mockito.mock(ArangoDriver.class);
        Mockito.doReturn(gson.fromJson(collectionLabels, Set.class)).when(query.arangoDriver).getCollectionLabels();
        query.namingConvention = new ArangoNamingConvention();
        this.whitelistedOrganizations = new LinkedHashSet<>();

    }

    @Test
    public void shouldAddTraversalWithBindVarAsCollection(){
        this.queryBuilder = new ArangoQueryBuilder(this.testSpecification, size, start, search, "", whitelistedOrganizations, null);
        String nameColl = "test";
        this.queryBuilder.addTraversal(false, nameColl);
        String expected = this.queryWithOrgHeader + ", OUTBOUND @@traversalCollection0";
        assert(this.queryBuilder.bindVariables.extractMap().size() == 1);
        assert(this.queryBuilder.bindVariables.extractMap().containsKey("@traversalCollection0"));
        assert(this.queryBuilder.bindVariables.extractMap().containsValue(nameColl));
        assert(this.queryBuilder.sb.toString().equals(expected));
    }

    @Test
    public void shouldAddSortByLeafFieldWithBindVarAsObjectProperty(){
        this.queryBuilder = new ArangoQueryBuilder(this.testSpecification, size, start, search, "", whitelistedOrganizations, null);
        String nameColl = "test";
        Set<String> fields = new HashSet<>();
        fields.add(nameColl);
        this.queryBuilder.addSortByLeafField(fields);
        String expected = this.queryWithOrgHeader + "  SORT root_doc.@sortByLeafField0 ASC";
        assert(this.queryBuilder.bindVariables.extractMap().size() == fields.size());
        assert(this.queryBuilder.bindVariables.extractMap().containsKey("sortByLeafField0"));
        assert(this.queryBuilder.bindVariables.extractMap().containsValue(nameColl));
        assert(this.queryBuilder.sb.toString()
                .replaceAll(" ", "").replaceAll("\n", "")
                .equals(expected.replaceAll(" ", "").replaceAll("\n", "")));
    }


}
