# Space Slack Channel Tunnel

The application provides a way to tunnel messages between channels in Space and Slack. You can install it from Space
Marketplace [here](https://plugins.jetbrains.com/plugin/19410-slack-channel-tunnel-beta).

You can also build and deploy the application on your own, for that please read on.

## Issue tracking

Reactions are not synchronized yet, it is a work in progress.

Please report the issues you find [here](https://youtrack.jetbrains.com/issues/SPACE).

## Application deployment

To deploy the application please do the following:

1. Build and deploy the application to a host available both to Slack and to your Space instance
2. Register a corresponding application in Slack
3. Install the application to Space using an install-link

### 1. Build and deploy the application

To build the application run the `distZip` gradle task. The application will be at `app/build/distributions/space-slack-sync.zip`.

The application is intended to be built and run with JDK 11. Other JDK versions have not been tested.

Make sure that the deployed application is available for HTTP requests from/to Slack and from/to your Space instance.

### 2. Register an application in Slack

If you don't yet have a Slack workspace, please create one. Using the Slack account from that workspace please
create an app on [api.slack.com](https://api.slack.com).

To set up the application please use the manifest specified below. Replace the host address placeholders with the
actual value for your deployed application.  

```yaml
display_information:
  name: JetBrains Space Slack Channel Sync
  description: Two-way synchronization of channels in Slack and Space
  background_color: "#1a181a"
features:
  bot_user:
    display_name: JetBrains Space Slack Channel Sync
    always_online: false
oauth_config:
  redirect_urls:
    - https://<HOST_ADDRESS_OF_THE_DEPLOYED_APP>/api/slack/oauth/callback
  scopes:
    bot:
      - users.profile:read
      - users:read
      - users:read.email
      - channels:history
      - team:read
      - chat:write
      - chat:write.customize
settings:
  event_subscriptions:
    request_url: https://<HOST_ADDRESS_OF_THE_DEPLOYED_APP>/api/slack/events
    bot_events:
      - message.channels
  org_deploy_enabled: false
  socket_mode_enabled: false
  token_rotation_enabled: true
```

### 3. Install the application to Space using an install-link

Create an installation link for you application by using the following URL template:

```
https://<HOST_ADDRESS_OF_YOUR_SPACE_INSTANCE>/extensions/installedApplications/new?name=Channel%20Tunnel%20Test&endpoint=https%3A%2F%2F<HOST_ADDRESS_OF_THE_DEPLOYED_APP_URL_ENCODED>%2Fapi%2Fspace&code-flow-enabled=true&code-flow-redirect-uris=https://nowhere.domain
```

Note that the host address of your app needs to be URL encoded as it is a part of a URL query parameter.

Paste the resulting URL into the browser address bar, and you should be taken to the app-install-dialog 
for the Space Slack Channel Sync app.

## Developing the application

If you'd like to make changes to the application and need to set up a local development environment, please follow
the steps below.

### Run application server locally

There are two parts of the application: client-side and server-side. You can run them separately, this lets
you make changes to the client code and see the effect in your browser immediately after webpage refresh.
The production build bundles client- and server-sides together into a single JAR (`distZip` gradle task).

The client side can be run using `npm`:
- to initialize node modules, run `npm install` in the `./client` directory. You only need to do this once.
- to run the client and serve the static content, run `npm run start`. In IntelliJ IDEA you can do this by clicking
  on a corresponding `Run` icon in `./client/package.json` file.

The server side can be run using the gradle `run` task: `./gradlew run`.

When you locally run client and server sides, you run two separate servers:
- webpack development server (`npm run start`) that hosts static content
- JVM server that processes API calls (`./gradlew run`)

In this configuration webpack dev server proxies the incoming API requests to JVM. The rules for the proxy can be found in
`./client/src/setupProxy.js`. Thus, an API call made from your browser will go this way:

1. Your browser makes API request, for example, to `HTTP GET https://1877-4533-142-9733-71.eu.ngrok.io/api/homepage-data`.
2. Request is forwarded to the ngrok instance running locally
3. Forwarded to webpack dev-server (`localhost:3000`, 3000 being the default port for webpack dev-server)
4. Forwarded to application server (`localhost:8080`, 8080 being the default port for application server)

When running ngrok, specify `--host-header` parameter:

```shell
ngrok http 3000 --host-header="localhost:3000"
```
It is required for the proxy in webpack dev server to function properly.

### Step by step

* Make sure you have Docker compose installed locally - it is needed for running the local DynamoDB container;
* Set up a Slack application, using the manifest template above
* Run ngrok as specified above
* Go to your application settings in Slack and modify all the urls there to point to ngrok tunnel url.
  The most convenient way to do this is via `App Manifest` tab (https://app.slack.com/app-settings/<team-id>/<app-id>/app-manifest) because
  all the urls are there in one place. You need to fix three urls - redirect url for OAuth, request url for event subscriptions and request url for interactivity.
* Copy `template_local.properties` file in the project root into the `local.properties` file (this isn't intended to be committed to git, so it's included in gitignore);
* Edit the `local.properties` file to provide proper configuration values:
    * Specify `app.encryptionKey`: the key for encrypting secrets that are stored in database. A simple way to do this is to generate the key
      https://www.allkeysgenerator.com/Random/Security-Encryption-Key-Generator.aspx (`Encryption key` tab, `256-bit` option).
    * Specify `slack.clientId`, `slack.clientSecret`, `slack.signingSecret` and `slack.appId`. They are generated by Slack when creating the application
      and can be found in the `Basic information` -> `App Credentials` section of the application page (https://api.slack.com/apps/<app-id>/general).
    * Specify `app.spacePublicUrl` if you have a Space instance that is not open to the internet, but is available locally from your machine
* Start the server application by `./gradlew run`
* Run `npm install` in the `./client` directory (only need to do this once)
* Run `npm run start` in the `./client` directory — this starts the webpack dev-server
* Create an installation URL for your application that allows you to install the app into Space:

```
https://<SPACE_HOSTNAME>/extensions/installedApplications/new?name=Slack%20Channel%20Tunnel&endpoint=https%3A%2F%2F<NGROK_HOSTNAME_URL_ENCODED>%2Fspace%2Fapi&code-flow-enabled=true&code-flow-redirect-uris=https%3A%2F%2Fnowhere.domain
```

Substitute the variables in the URL template and paste the resulting URL into the browser. It will take you to the
app installation screen.

Remember to reconfigure the Slack application settings and then reinstall the application to both Slack workspace and Space organization
whenever the ngrok tunnel is reestablished with a new address. Space allows multiple applications with the same name, so it's better to drop
the previous installations from the test organization before installing the application afresh.

# License

`Space Slack Channel Tunnel` is distributed under the terms of the Apache License (Version 2.0).