package org.humanbrainproject.knowledgegraph.entity.indexing;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

public class QualifiedGraphIndexingSpecTest {

    @Test
    public void getOrganization() {

        GraphIndexingSpec s = Mockito.mock(GraphIndexingSpec.class);
        Mockito.doReturn("organization/domain/schema/v0.0.1").when(s).getEntityName();
        QualifiedGraphIndexingSpec spec = new QualifiedGraphIndexingSpec(s, Collections.emptyMap(), Collections.emptyList());
        String organization = spec.getOrganization();
        Assert.assertEquals("organization", organization);

    }
}