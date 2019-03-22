package org.humanbrainproject.knowledgegraph.nexus.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.nexus.control.NexusBatchUploader;
import org.humanbrainproject.knowledgegraph.nexus.entity.FileStructureData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NexusUtils {

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    NexusBatchUploader uploader;


    public void uploadFileStructure(FileStructureData fs){
        uploader.uploadFileStructure(fs);
    }
}
