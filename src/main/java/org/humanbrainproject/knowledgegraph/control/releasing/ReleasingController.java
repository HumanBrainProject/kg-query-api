package org.humanbrainproject.knowledgegraph.control.releasing;

import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.control.arango.DatabaseController;
import org.humanbrainproject.knowledgegraph.entity.indexing.Release;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReleasingController {

    @Autowired
    public DatabaseController controller;

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoNamingConvention namingConvention;

    public Set<String> findDocumentIdsToBeUnreleased(Release release) {
        return release.getVertices().stream().map(v -> {
            List<JsonLdEdge> edgesToBeRemoved = repository.getEdgesToBeRemoved(v, controller.getDefaultDB());
            return edgesToBeRemoved.stream().map(e -> repository.getTargetVertexId(e, controller.getDefaultDB())).collect(Collectors.toList());
        }).flatMap(List::stream).collect(Collectors.toSet());
    }

    public void unreleaseDocumentsById(Set<String> documentIdsToBeUnreleased) {
        if (documentIdsToBeUnreleased != null) {
            documentIdsToBeUnreleased.forEach(d -> unreleaseInstance(d, false));
        }
    }

    public void releaseVertices(Release release) {
        if(release!=null) {
            List<String> releaseInstances = release.getReleaseInstances();
            for (String releaseInstance : releaseInstances) {
                releaseInstance(releaseInstance);
            }
        }
    }

    public void unreleaseVertices(Release release) {
        if(release!=null) {
            List<String> releaseInstances = release.getReleaseInstances();
            for (String releaseInstance : releaseInstances) {
                unreleaseInstance(releaseInstance, true);
            }
        }
    }

    private void releaseInstance(String identifier) {
        if(identifier!=null){
            Set<String> edgesCollectionNames = controller.getDefaultDB().getEdgesCollectionNames();
            Set<String> embeddedInstances = repository.getEmbeddedInstances(Collections.singletonList(identifier), controller.getDefaultDB(), edgesCollectionNames, new LinkedHashSet<>());
            repository.stageElementsToReleased(embeddedInstances, controller.getDefaultDB(), controller.getReleasedDB());
        }
    }

    public void unreleaseInstance(String url, boolean requireHttp) {
        //The url needs to be absolute - everything else is not resolvable.
        if (!requireHttp || url.startsWith("http")) {
            Set<String> edgesCollectionNames = controller.getReleasedDB().getEdgesCollectionNames();
            Set<String> embeddedInstances = repository.getEmbeddedInstances(Collections.singletonList(url), controller.getReleasedDB(), edgesCollectionNames, new LinkedHashSet<>());
            for (String embeddedInstance : embeddedInstances) {
                repository.deleteVertex(embeddedInstance, controller.getReleasedDB());
            }
        }
    }

}
