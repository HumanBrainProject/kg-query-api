package org.humanbrainproject.knowledgegraph.control.inference;

import org.humanbrainproject.knowledgegraph.entity.indexing.QualifiedGraphIndexingSpec;
import org.humanbrainproject.knowledgegraph.entity.indexing.TodoList;

public interface InferenceStrategy {

    TodoList infer(QualifiedGraphIndexingSpec spec);

    boolean isInferenceNeeded(QualifiedGraphIndexingSpec spec);


}
