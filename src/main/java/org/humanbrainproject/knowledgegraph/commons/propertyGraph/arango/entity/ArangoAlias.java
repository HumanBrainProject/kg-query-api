package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;

import java.util.Objects;

public class ArangoAlias {

    private final String name;

    public ArangoAlias(String name) {
        this.name = name;
    }

    public static ArangoAlias fromOriginalFieldName(String fieldName){
        return new ArangoAlias(ArangoNamingHelper.reduceStringToMaxSizeByHashing(ArangoNamingHelper.replaceSpecialCharacters(fieldName).replaceAll("-", "_")));
    }

    public static ArangoAlias fromSpecField(SpecField specField){
        return fromOriginalFieldName(specField.fieldName);
    }

    public static ArangoAlias fromLeafPath(SpecTraverse specTraverse){
        return new ArangoAlias(specTraverse.pathName);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoAlias that = (ArangoAlias) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
