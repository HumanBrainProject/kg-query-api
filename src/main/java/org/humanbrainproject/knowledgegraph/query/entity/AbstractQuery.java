package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

import java.util.Set;
import java.util.stream.Collectors;

@ToBeTested(easy = true)
public abstract class AbstractQuery {

    private final NexusSchemaReference schemaReference;
    private Filter filter = new Filter();
    private Pagination pagination = new Pagination();
    private final String vocabulary;

    public AbstractQuery(NexusSchemaReference schemaReference, String vocabulary) {
        this.schemaReference = schemaReference;
        this.vocabulary = vocabulary;
    }

    protected AbstractQuery(NexusSchemaReference schemaReference, String vocabulary, Filter filter, Pagination pagination){
        this.schemaReference = schemaReference;
        this.vocabulary = vocabulary;
        this.filter = filter;
        this.pagination = pagination;
    }

    public NexusSchemaReference getSchemaReference() {
        return schemaReference;
    }

    public String getVocabulary() {
        return vocabulary;
    }

    public Filter getFilter() {
        return filter;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public Set<NexusInstanceReference> getInstanceReferencesWhitelist(){
        return getFilter().getRestrictToIds()!=null ? getFilter().getRestrictToIds().stream().map(id -> new NexusInstanceReference(schemaReference, id)).collect(Collectors.toSet()) : null;
    }

    public Set<ArangoDocumentReference> getDocumentReferenceWhitelist(){
        Set<NexusInstanceReference> instanceReferencesWhitelist = getInstanceReferencesWhitelist();
        return instanceReferencesWhitelist!=null ? instanceReferencesWhitelist.stream().map(ArangoDocumentReference::fromNexusInstance).collect(Collectors.toSet()) : null;
    }

}
