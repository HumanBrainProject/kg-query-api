package org.humanbrainproject.knowledgegraph.control;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class Configuration {

    @Value("${org.humanbrainproject.knowledgegraph.nexus_base}")
    private String nexusBase;

    private static final String NEXUS_VOCAB_SUBPATH = "vocabs/nexus/core/terms/v0.1.0";

    public String getNexusBase() {
        return nexusBase;
    }

    private String getNexusVocabProperty(String property){
        return String.format("%s/%s/%s", nexusBase, NEXUS_VOCAB_SUBPATH, property);
    }

    public String getRev(){
        return getNexusVocabProperty("rev");
    }

    public String getSchema(){
        return getNexusVocabProperty("schema");
    }

    public String getDeprecated(){
        return getNexusVocabProperty("deprecated");
    }

    public String getUUID(){
        return getNexusVocabProperty("uuid");
    }

}
