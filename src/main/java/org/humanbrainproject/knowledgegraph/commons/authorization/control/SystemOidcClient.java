package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

@Component
@ToBeTested(systemTestRequired = true)
public class SystemOidcClient {

    private final String KEYCLOAK_CONFIG = "keycloak_config";
    private final String ACCESS_TOKEN_KEY = "access_token";
    private OidcAccessToken currentToken;


    @Value("${org.humanbrainproject.knowledgegraph.oidc.configFile}")
    String oidcConfigFile;

    Gson gson = new Gson();
    Map openIdConfig;
    Map clientConfig;


    @PostConstruct
    public void init() {
        clientConfig = readConfigFile();
        RestTemplate template = new RestTemplate();
        String openidconf = template.getForObject((String) clientConfig.get(KEYCLOAK_CONFIG), String.class);
        openIdConfig = gson.fromJson(openidconf, Map.class);
    }

    public String getClientId(){
        return (String) clientConfig.get("keycloak_client_id");
    }

    public String getRealm(){
        return (String) clientConfig.get("keycloak_realm");
    }

    private Map readConfigFile() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(oidcConfigFile))) {
            return gson.fromJson(bufferedReader, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Was not able to read the configuration file!", e);
        }
    }

    private String getToken() {
        String tokenUrl = (String) openIdConfig.get("token_endpoint");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", getClientId());
        params.add("client_secret", (String) clientConfig.get("keycloak_client_secret"));
        HttpEntity<Map> request = new HttpEntity<>(params, headers);
        RestTemplate template = new RestTemplate();
        return template.postForObject(tokenUrl, request, String.class);
    }

    public void refreshToken() {
        String token = getToken();
        Map tokenResponse = gson.fromJson(token, Map.class);
        this.currentToken = new OidcAccessToken().setToken((String) tokenResponse.get("access_token"));
    }

    public Map<String, Object> getUserInfo(OidcAccessToken token) {
        String url = (String) openIdConfig.get("userinfo_endpoint");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return res.getBody();
    }

    public OidcAccessToken getAuthorizationToken() {
        if (currentToken == null) {
            refreshToken();
        }
        return this.currentToken;
    }

}
