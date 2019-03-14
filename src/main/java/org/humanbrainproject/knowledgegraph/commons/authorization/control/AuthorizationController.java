package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.InternalMasterKey;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

/**
 * The authorization controller is responsible to m
 */
@Component
@Tested
public class AuthorizationController {


    @Autowired
    SystemOidcHeaderInterceptor systemOidcHeaderInterceptor;

    public ClientHttpRequestInterceptor getInterceptor(Credential credential){
        if(credential instanceof InternalMasterKey){
            return systemOidcHeaderInterceptor;
        }
        else if(credential instanceof OidcAccessToken){
            return new OidcHeaderInterceptor((OidcAccessToken)credential);
        }
        throw new RuntimeException("Unknown credential: "+credential);
    }

}
