package org.humanbrainproject.knowledgegraph.structure.boundary;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StructureAtStartup implements InitializingBean {

    @Autowired
    Structure structure;

    @Override
    public void afterPropertiesSet() throws Exception {
        structure.refreshStructuredCachesAtStartup();
    }
}
