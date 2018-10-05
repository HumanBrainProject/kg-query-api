package org.humanbrainproject.knowledgegraph.entity.indexing;

import java.util.List;

public class EditorInstance extends GraphEntity{

    public static final String ORIGINAL_PARENT_PROPERTYNAME = "http://hbp.eu/manual#parent";

    public EditorInstance(QualifiedGraphIndexingSpec spec, String type) {
        super(spec, type);
    }

    public String getUrl(){
        return this.spec.getUrl();
    }


    public List<String> getParents(){
        return getReferencesForLinkedInstances(spec.getMap().get(ORIGINAL_PARENT_PROPERTYNAME), true);
    }

    @Override
    public boolean isInstance() {
        return this.spec.isEditorInstance();
    }

    public boolean hasOriginalParent(){
        List<String> parents = getParents();
        return parents !=null && !parents.isEmpty();
    }

}
