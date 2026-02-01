package com.ichwan.sfdcevent;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriberCDC {

    private final SalesforceAuthService authService;
    private final SalesforceQueryService queryService;

    @Value("${salesforce.api-version}")
    private String apiVersion;

    private BayeuxClient client;
    private HttpClient httpClient;

    @PostConstruct
    public void start() throws Exception {
        authService.authenticate();

        String cometdUrl = authService.getInstanceUrl() + "/cometd/" + apiVersion;

        httpClient = new HttpClient();
        httpClient.start();

        Map<String, Object> options = new HashMap<>();
        JettyHttpClientTransport transport = new JettyHttpClientTransport(options, httpClient) {
            @Override
            protected void customize(Request request) {
                request.headers(header -> header.add("Authorization","Bearer "+authService.getAccessToken()));
            }
        };

        client = new BayeuxClient(cometdUrl, transport);
        client.getChannel(Channel.META_HANDSHAKE)
                .addListener((ClientSessionChannel.MessageListener) (clientSessionChannel, message) -> {
                    log.debug("handshake success: {}", message.isSuccessful());

                    if (!message.isSuccessful()) log.debug("handshake error: {}", message);
                });
        client.handshake();

        if (!client.waitFor(10_000, BayeuxClient.State.CONNECTED)) throw new IllegalStateException("failed connect to salesforce");

        subscribe();
    }

    private void subscribe() {
        client.getChannel("/data/AppLog__ChangeEvent")
                .subscribe((ch, msg) -> {
                    log.info("CDC Successfully subscribe: {}", msg.getData());
                    asMap(msg.getData()).flatMap(data -> asMap(data.get("payload"))).ifPresent(payload -> {
                        asMap(payload.get("ChangeEventHeader")).ifPresent(header -> {
                            String changeType = (String) header.get("ChangeType");
                            String recordId = ((List<String>) header.get("recordIds")).get(0);
                            log.info("CDC changeType={}, recordId={}", changeType, recordId);
                            handleChange(changeType, recordId);
                        });
                    });
                });
    }

    private void handleChange(String changeType, String recordId) {
        if ("DELETE".equals(changeType)) {
            log.info("skip fetch for DELETE {}",recordId);
        }

        Map<String, Object> logById = queryService.getAppLogById(recordId);
        if (logById != null) log.info("process applog record {}", logById);
    }

    private Optional<Map<String, Object>> asMap(Object obj) {
        return (obj instanceof Map) ? Optional.of((Map<String, Object>) obj) : Optional.empty();
    }

    @PreDestroy
    public void shutdown() throws Exception {
        if (client != null) client.disconnect();
        if (httpClient != null) httpClient.stop();
    }
}
