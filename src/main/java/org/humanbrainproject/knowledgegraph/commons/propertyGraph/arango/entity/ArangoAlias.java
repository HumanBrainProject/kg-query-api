package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;

import java.util.Objects;

public class ArangoAlias {

    private final String originalName;

    public ArangoAlias(String name) {
        this.originalName = name;
    }

    public static ArangoAlias fromOriginalFieldName(String fieldName){
        return new ArangoAlias(fieldName);
    }

    public static ArangoAlias fromSpecField(SpecField specField){
        return new ArangoAlias(specField.fieldName);
    }

    public static ArangoAlias fromLeafPath(SpecTraverse specTraverse){
        return new ArangoAlias(specTraverse.pathName);
    }

    public String getArangoName() {
        return ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(originalName).replaceAll("-", "_"));
    }

    public String getOriginalName() {
        return originalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoAlias that = (ArangoAlias) o;
        return Objects.equals(originalName, that.originalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalName);
    }
}
