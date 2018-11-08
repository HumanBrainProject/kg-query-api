package org.humanbrainproject.knowledgegraph.commons.nexus.control;


import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusRelativeUrl;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * The system nexus client makes use of its technical user account and therefore has typically more rights than the user
 * invoking API calls, etc. PLEASE BE CAREFUL in the use of this service, since use in the wrong place might expose data
 * we wouldn't want to be accessible for users.
 */
@Component
public class SystemNexusClient {

    @Autowired
    NexusConfiguration configuration;

    @Autowired
    SystemOidcHeaderInterceptor systemOidc;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    InstanceController instanceController;

    public List<Map> find(NexusSchemaReference reference, String fieldName, String fieldValue) {
        return nexusClient.find(reference, fieldName, fieldValue, systemOidc.getToken());
    }

    public Map get(NexusRelativeUrl relativeUrl) {
        return nexusClient.get(relativeUrl, systemOidc.getToken());
    }

    public Map put(NexusRelativeUrl relativeUrl, Integer revision, Map payload){
        return nexusClient.put(relativeUrl, revision, payload, systemOidc.getToken());
    }

    public Map post(NexusRelativeUrl relativeUrl, Integer revision, Map payload)  {
        return nexusClient.post(relativeUrl, revision, payload, systemOidc.getToken());
    }

    public Map patch(NexusRelativeUrl relativeUrl, Integer revision, Map payload) {
        return nexusClient.patch(relativeUrl, revision, payload, systemOidc.getToken());
    }

    public final String getPayload(NexusInstanceReference nexusInstanceReference) {
        return nexusClient.get(nexusInstanceReference.getRelativeUrl(), systemOidc.getToken(), String.class);
    }

    public NexusInstanceReference createOrUpdateInstance(NexusInstanceReference nexusInstanceReference, Map<String, Object> payload) {
        return instanceController.createInstanceByNexusId(nexusInstanceReference.getNexusSchema(), nexusInstanceReference.getId(), nexusInstanceReference.getRevision(), payload, systemOidc.getToken());
    }

}
