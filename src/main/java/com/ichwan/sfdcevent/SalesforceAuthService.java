package com.ichwan.sfdcevent;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class SalesforceAuthService {

    @Value("${salesforce.login-url}")
    private String loginUrl;

    @Value("${salesforce.client-id}")
    private String clientId;

    @Value("${salesforce.client-secret}")
    private String clientSecret;

    @Value("${salesforce.grant_type}")
    private String grantType;

    @Getter
    private String accessToken;
    @Getter
    private String instanceUrl;

    public void authenticate() throws Exception {
        String tokenEndpoint = loginUrl + "/services/oauth2/token";

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", grantType);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenEndpoint, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        this.accessToken = (String) responseBody.get("access_token");
        this.instanceUrl = (String) responseBody.get("instance_url");
    }

}
