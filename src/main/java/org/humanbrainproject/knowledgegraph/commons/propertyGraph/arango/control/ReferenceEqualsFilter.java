package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;

public class ReferenceEqualsFilter extends EqualsFilter{

    public ReferenceEqualsFilter(String key, String value) {
        super(AQL.trust(AQL.preventAqlInjection(key).getValue()+"`.`"+ JsonLdConsts.ID), value);
    }
}
