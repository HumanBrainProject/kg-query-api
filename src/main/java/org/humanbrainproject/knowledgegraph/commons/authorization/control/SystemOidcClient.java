/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

@Component
@ToBeTested(systemTestRequired = true)
public class SystemOidcClient {

    private final String OPENID_HOST_KEY = "openid_host";
    private final String TOKEN_KEY = "token_endpoint";
    private final String USER_INFO = "userinfo_endpoint";
    private final String RELATIVE_OPENID_CONFIGURATION_URL = ".well-known/openid-configuration";
    private final String ACCESS_TOKEN_KEY = "access_token";
    private OidcAccessToken currentToken;


    @Value("${org.humanbrainproject.knowledgegraph.oidc.configFile}")
    String oidcConfigFile;

    final Gson gson = new Gson();

    private String getTokenUrl(String host) {
       return getUrlFromConfig(host, TOKEN_KEY);
    }

    private String getUrlFromConfig(String host, String key){
        RestTemplate template = new RestTemplate();
        String openidconf = template.getForObject(String.format("%s/%s", host, RELATIVE_OPENID_CONFIGURATION_URL), String.class);
        Map map = gson.fromJson(openidconf, Map.class);
        return map.get(key).toString();
    }

    private Map readConfigFile(){
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(oidcConfigFile))) {
            return gson.fromJson(bufferedReader, Map.class);
        }
        catch (IOException e){
            throw new RuntimeException("Was not able to read the configuration file!", e);
        }
    }

    private String getToken(Map map, String tokenUrl){
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

    public void refreshToken(){
        Map map = readConfigFile();
        String host = map.get(OPENID_HOST_KEY).toString();
        String tokenUrl = getTokenUrl(host);
        String token = getToken(map, tokenUrl);
        Map tokenResponse = gson.fromJson(token, Map.class);
        this.currentToken = new OidcAccessToken().setToken(tokenResponse.get(ACCESS_TOKEN_KEY).toString());
    }

    @Cacheable("userInfoByToken")
    public UserInformation getUserInfo(OidcAccessToken token){
        Map map = readConfigFile();
        String host = map.get(OPENID_HOST_KEY).toString();
        String url =  getUrlFromConfig(host, USER_INFO);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return new UserInformation(res.getBody());
    }

    @CacheEvict(allEntries = true, cacheNames = "userInfoByToken")
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
    public void clearUserInfoTokenCache() {
    }


    public OidcAccessToken getAuthorizationToken(){
        if(currentToken==null) {
            refreshToken();
        }
        return this.currentToken;
    }

}
