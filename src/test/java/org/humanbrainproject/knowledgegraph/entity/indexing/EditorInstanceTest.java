package org.humanbrainproject.knowledgegraph.entity.indexing;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class EditorInstanceTest {

    @Test
    public void isInstance() {
        QualifiedGraphIndexingSpec spec = Mockito.mock(QualifiedGraphIndexingSpec.class);
        Mockito.doReturn("fooeditor").when(spec).getOrganization();
        EditorInstance manualSpace = new EditorInstance(spec, null);
        boolean result = manualSpace.isInstance();
        Assert.assertTrue(result);
    }

    @Test
    public void isInstanceNOK() {
        QualifiedGraphIndexingSpec spec = Mockito.mock(QualifiedGraphIndexingSpec.class);
        Mockito.doReturn("foo").when(spec).getOrganization();
        EditorInstance manualSpace = new EditorInstance(spec, null);
        boolean result = manualSpace.isInstance();
        Assert.assertFalse(result);
    }
}