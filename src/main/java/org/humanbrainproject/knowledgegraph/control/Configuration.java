package org.humanbrainproject.knowledgegraph.control;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class Configuration {

    private static final String DEFAULT_NAMESPACE = "http://schema.hbp.eu/kgquery#";

    @Value("${org.humanbrainproject.knowledgegraph.nexus_base}")
    private String nexusBase;


    public String getNexusBase() {
        return nexusBase;
    }

    public String getRev(){
        return String.format("%s%s", DEFAULT_NAMESPACE, "rev");
    }


}
