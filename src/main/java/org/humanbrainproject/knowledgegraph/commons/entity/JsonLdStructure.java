package org.humanbrainproject.knowledgegraph.commons.entity;

import org.apache.commons.lang3.StringUtils;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;

public class JsonLdStructure<T extends JsonLdObject> {

    private final NexusSchemaReference nexusSchemaReference;
    private final Class<T> entityClass;

    public JsonLdStructure(NexusSchemaReference nexusSchemaReference, Class<T> entityClass) {
        this.nexusSchemaReference = nexusSchemaReference;
        this.entityClass = entityClass;
    }

    public final String getType(){
        return getNamespace()+StringUtils.capitalize(nexusSchemaReference.getSchema());
    }

    public final String getNamespace(){
        return HBPVocabulary.NAMESPACE+nexusSchemaReference.getOrganization()+"/";
    }

    public final NexusSchemaReference getNexusSchemaReference() {
        return nexusSchemaReference;
    }

    public final String getFieldNameInNamespace(String fieldName){
        return getNamespace()+fieldName;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }
}
