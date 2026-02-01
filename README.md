
This repository contains a simple CDC subscriber that authenticates to Salesforce using OAuth2, connects to the Streaming/CometD endpoint and listens for CDC events.

## What is Change Data Capture (CDC)?

Change Data Capture (CDC) is a Salesforce feature that publishes changes (create, update, delete, undelete) to Salesforce records as events. Clients can subscribe to these events to react in near real-time.

## Project flow

1. Authenticate with Salesforce OAuth2 (username/password or connected app flow) to obtain an access token and instance URL. See SalesforceAuthService.java for the implementation.
2. Build the CometD endpoint URL from the instance URL: `${instanceUrl}/cometd/<api-version>`.
3. Connect a CometD/Bayeux client and perform the handshake using the OAuth access token in the `Authorization: Bearer <token>` header.
4. Subscribe to CDC channels (for example `/data/ChangeEvents` or object-specific channels like `/data/AccountChangeEvent`).
5. Receive and handle events in the subscriber (process, store, or forward as needed).

## How this project works (high level)

- SalesforceAuthService authenticates against the configured login URL and fills accessToken and instanceUrl.
- A CometD-based subscriber (see SubscriberCDC.java) connects to the Salesforce Streaming API and subscribes to CDC channels.
- Received change events can be processed inside the subscriber callback.

## Configuration

Set values in `src/main/resources/application.yaml` (or environment variables) for:

- `salesforce.login-url` — Salesforce login URL (e.g. `https://login.salesforce.com` or sandbox `https://test.salesforce.com`).
- `salesforce.client-id` — Connected App client id.
- `salesforce.client-secret` — Connected App client secret.
- `salesforce.grant_type` — OAuth grant type (e.g. `password` for username-password flow or other flows you use).

Make sure your Connected App has the correct OAuth scopes and the Streaming API feature is enabled in your org.

## Build and run

- Build: `./mvnw -DskipTests package`
- Run: `./mvnw spring-boot:run`

Or run the generated jar in `target/`:

- `java -jar target/sfdcevent-<version>.jar`

## Useful references

- Salesforce Change Data Capture (CDC) — https://developer.salesforce.com/docs/atlas.en-us.change_data_capture.meta/change_data_capture/
- Streaming API / CometD — https://developer.salesforce.com/docs/atlas.en-us.streaming_api.meta/streaming_api/
- Streaming API: Change Data Capture — https://developer.salesforce.com/docs/atlas.en-us.change_data_capture.meta/change_data_capture/change_data_capture_streams.htm
- Salesforce OAuth 2.0 endpoints and flows — https://help.salesforce.com/s/articleView?id=sf.remoteaccess_oauth_web_server_flow.htm
- CometD Java client — https://cometd.org/documentation/clients#_java

## Notes

- This project is an example and not production hardened. Add retry/backoff, error handling, and secure secret management before using in production.




