package com.ichwan.sfdcevent;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriberCDC {

    private final SalesforceAuthService authService;

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
                .addListener(new ClientSessionChannel.MessageListener() {
                    @Override
                    public void onMessage(ClientSessionChannel clientSessionChannel, Message message) {
                        log.debug("handshake success: {}", message.isSuccessful());

                        if (!message.isSuccessful()) log.debug("handshake error: {}", message);
                    }
                });
        client.handshake();

        if (!client.waitFor(10_000, BayeuxClient.State.CONNECTED)) throw new IllegalStateException("failed connect to salesforce");

        subscribe();
    }

    private void subscribe() {
        client.getChannel("/data/AppLog__ChangeEvent")
                .subscribe((ch, msg) -> {
                    log.info("CDC Successfully subscribe: {}", msg.getData());
                });
    }

    @PreDestroy
    public void shutdown() throws Exception {
        if (client != null) client.disconnect();
        if (httpClient != null) httpClient.stop();
    }
}
