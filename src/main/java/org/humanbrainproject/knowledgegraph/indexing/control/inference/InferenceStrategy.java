package org.humanbrainproject.knowledgegraph.indexing.control.inference;

import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;

import java.util.Set;

public interface InferenceStrategy {

    /**
     * @param message   - the original message triggering the inference mechanism
     * @param documents - a collection container to add all documents that have been created / generated as inference instances. Implementing classes should take into account, that there can already be instances from previous inference strategies and therefore ensure to reuse them.
     */
    void infer(QualifiedIndexingMessage message, Set<MainVertex> documents);

}
