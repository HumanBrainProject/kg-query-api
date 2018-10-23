package org.humanbrainproject.knowledgegraph.authorization.control;

import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.authorization.entity.OidcAccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

@Component
public class OidcClient {

    private final String OPENID_HOST_KEY = "openid_host";
    private final String TOKEN_KEY = "token_endpoint";
    private final String RELATIVE_OPENID_CONFIGURATION_URL = ".well-known/openid-configuration";
    private final String ACCESS_TOKEN_KEY = "access_token";

    @Value("${org.humanbrainproject.knowledgegraph.oidc.configFile}")
    String oidcConfigFile;

    final Gson gson = new Gson();

    String getTokenUrl(String host) {
        RestTemplate template = new RestTemplate();
        String openidconf = template.getForObject(String.format("%s/%s", host, RELATIVE_OPENID_CONFIGURATION_URL), String.class);
        Map map = gson.fromJson(openidconf, Map.class);
        return map.get(TOKEN_KEY).toString();
    }


    Map readConfigFile() throws IOException{
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(oidcConfigFile))) {
            return gson.fromJson(bufferedReader, Map.class);
        }
    }

    String getToken(Map map, String tokenUrl){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        for (Object o : map.keySet()) {
            if (!OPENID_HOST_KEY.equals(o)) {
                params.add(o.toString(), map.get(o).toString());
            }
        }
        params.add("grant_type", "refresh_token");
        HttpEntity<Map> request = new HttpEntity<>(params, headers);
        RestTemplate template = new RestTemplate();
        return template.postForObject(tokenUrl, request, String.class);
    }


    public OidcAccessToken getAuthorizationToken() throws IOException{
        Map map = readConfigFile();
        String host = map.get(OPENID_HOST_KEY).toString();
        String tokenUrl = getTokenUrl(host);
        String token = getToken(map, tokenUrl);
        Map tokenResponse = gson.fromJson(token, Map.class);
        return new OidcAccessToken().setToken(tokenResponse.get(ACCESS_TOKEN_KEY).toString());
    }

}
