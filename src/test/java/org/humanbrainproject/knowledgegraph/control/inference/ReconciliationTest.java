package org.humanbrainproject.knowledgegraph.control.inference;

import org.humanbrainproject.knowledgegraph.entity.indexing.QualifiedGraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.factories.QualifiedGraphIndexingSpecFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ReconciliationTest {

    Reconciliation reconciliation;

    @Before
    public void setup(){
        reconciliation = Mockito.spy(new Reconciliation());
    }

    @Test
    public void reconcileOriginalOnly() {
        QualifiedGraphIndexingSpec fooOriginal = QualifiedGraphIndexingSpecFactory.createQualifiedGraphIndexingSpecWithDummyPayload("foo/bar/foo/v0.0.1", "foo");
        reconciliation.infer(fooOriginal);

        // Assumed: virtual entity in index (reconciled space) -> not in Nexus
        // Assumed: virtual links pointing to virtual entity in reconciled space
    }



    @Test
    public void reconcileEditorOnly(){
        QualifiedGraphIndexingSpec fooEditor = QualifiedGraphIndexingSpecFactory.createQualifiedGraphIndexingSpecWithDummyPayload("fooeditor/bar/foo/v0.0.1", "foo");
        reconciliation.infer(fooEditor);

        // Assumed: virtual entity in index (reconciled space) -> not in Nexus
        // Assumed: virtual links pointing to virtual entity in reconciled space
    }

    @Test
    public void reconcileOriginalAndEditorTriggeredByOriginal(){
        QualifiedGraphIndexingSpec fooOriginal = QualifiedGraphIndexingSpecFactory.createQualifiedGraphIndexingSpecWithDummyPayload("foo/bar/foo/v0.0.1", "foo");
        QualifiedGraphIndexingSpec fooEditor = QualifiedGraphIndexingSpecFactory.createQualifiedGraphIndexingSpecWithDummyPayload("fooeditor/bar/foo/v0.0.1", "foo");

        reconciliation.infer(fooOriginal);

        // Assumed: persisted entity in reconciled space (persist to Nexus)
        // -> createOrUpdateReconciledInstance is triggered later through Nexus indexing mechanism
    }

    @Test
    public void reconcileOriginalAndEditorTriggeredByEditor(){
        QualifiedGraphIndexingSpec fooOriginal = QualifiedGraphIndexingSpecFactory.createQualifiedGraphIndexingSpecWithDummyPayload("foo/bar/foo/v0.0.1", "foo");
        QualifiedGraphIndexingSpec fooEditor = QualifiedGraphIndexingSpecFactory.createQualifiedGraphIndexingSpecWithDummyPayload("fooeditor/bar/foo/v0.0.1", "foo");

        // Assumed: persisted entity in reconciled space (persist to Nexus)
        // -> createOrUpdateReconciledInstance is triggered later through Nexus indexing mechanism

        reconciliation.infer(fooEditor);
    }


    @Test
    public void createOrUpdateReconciledInstance(){
        QualifiedGraphIndexingSpec fooReconciled = QualifiedGraphIndexingSpecFactory.createQualifiedGraphIndexingSpecWithDummyPayload("fooreconciled/bar/foo/v0.0.1", "foo");
        reconciliation.infer(fooReconciled);

        //Assumed: Replace already existing instance in index
        //Assumed: virtual links pointing to persisted entity in reconciled space

    }


    @Test
    public void createOrUpdateInstancePointingToReconciled(){
        QualifiedGraphIndexingSpec fooReconciled = QualifiedGraphIndexingSpecFactory.createQualifiedGraphIndexingSpecWithDummyPayload("fooreconciled/bar/foo/v0.0.1", "foo");
        reconciliation.infer(fooReconciled);

        //Assumed: Replace already existing instance in index
        //Assumed: virtual links pointing to persisted entity in reconciled space

    }
}