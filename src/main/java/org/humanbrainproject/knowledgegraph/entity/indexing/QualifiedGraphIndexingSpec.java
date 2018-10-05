package org.humanbrainproject.knowledgegraph.entity.indexing;

import org.humanbrainproject.knowledgegraph.control.SubSpaceName;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QualifiedGraphIndexingSpec{
    private final Map map;
    private final GraphIndexingSpec spec;
    private final List<JsonLdVertex> vertices;

    public String getUrl(){
        return String.format("%s/%s", spec.getEntityName(), spec.getId());
    }

    public String getOrganization(){
        return spec.getEntityName().split("/")[0];
    }


    public QualifiedGraphIndexingSpec(GraphIndexingSpec spec, Map map, List<JsonLdVertex> vertices){
        this.map = Collections.unmodifiableMap(map);
        this.vertices = vertices;
        this.spec = spec;
    }

    public GraphIndexingSpec getSpec() {
        return spec;
    }

    public Map getMap() {
        return map;
    }

    public List<JsonLdVertex> getVertices() {
        return vertices;
    }

    private SpatialAnchoring spatialAnchoring;

    public SpatialAnchoring asSpatialAnchoring(){
        if(spatialAnchoring==null) {
            spatialAnchoring = new SpatialAnchoring(this);
        }
        return spatialAnchoring.isInstance() ? spatialAnchoring : null;
    }

    private Release release;

    public Release asRelease(){
        if(release==null) {
            release = new Release(this);
        }
        return release.isInstance() ? release : null;
    }

    private EditorInstance editorInstance;

    public EditorInstance asEditorInstance(){
        if(editorInstance == null){
            editorInstance = new EditorInstance(this, null);
        }
        return editorInstance.isInstance() ? editorInstance : null;
    }

    public boolean isEditorInstance(){
        String organization = getOrganization();
        return organization != null && organization.endsWith(SubSpaceName.EDITOR.getName());
    }

    public boolean isReconciledInstance(){
        String organization = getOrganization();
        return organization != null && organization.endsWith(SubSpaceName.RECONCILED.getName());
    }

    public boolean isOriginalInstance(){
        return !isEditorInstance() && !isReconciledInstance();
    }


}
