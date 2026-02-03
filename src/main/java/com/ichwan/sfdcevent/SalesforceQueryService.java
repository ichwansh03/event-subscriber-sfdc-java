package com.ichwan.sfdcevent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesforceQueryService {

    private final SalesforceAuthService authService;

    @Value("${salesforce.api-version}")
    private String apiVersion;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getAppLogById(String recordId) {
        String soql = String.format("SELECT Id, Name, Status__c, Report_Name__c FROM AppLog__c WHERE Id = '%s'", recordId);
        String url = authService.getInstanceUrl()+"/services/data/v"+apiVersion+"/query?q="+soql;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, req, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null || ((List<?>) body.get("records")).isEmpty()) {
            log.warn("no app log found for id {}", recordId);
            return null;
        }

        Map<String, Object> record = (Map<String, Object>) ((List<?>) body.get("records")).get(0);
        log.info("fetched app log from salesforce: {}",record);

        return record;
    }
}
