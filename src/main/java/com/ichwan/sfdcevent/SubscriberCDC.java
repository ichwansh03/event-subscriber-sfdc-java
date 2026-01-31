package com.ichwan.sfdcevent;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SubscriberCDC {

    @Autowired
    private SalesforceAuthService authService;

    @Value("${salesforce.api-version}")
    private String apiVersion;

    private BayeuxClient client;
    private HttpClient httpClient;

    
}
