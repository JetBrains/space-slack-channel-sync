# Space Slack Channel Tunnel

The application provides a way to tunnel messages between channels in Space and Slack. You can install it from Space
Marketplace here: https://plugins.jetbrains.com/plugin/19410-slack-channel-tunnel-beta

You can also build and deploy the application on your own, for that please read on.

## Known issues

Reactions are not synchronized yet, it is a work in progress.

Please report the issues you find [here](https://youtrack.jetbrains.com/issues/SPACE).

## Making your own app deployment

To deploy the application please do the following:

1. Build and deploy the application to a host available both to Slack and to your Space instance
2. Register a corresponding application in Slack
3. Install the application to Space using an install-link

### 1. Build and deploy the application

To build the application run the `assemble` gradle task. The application will be at `build/distributions/space-slack-sync.zip`.

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

If you'd like to make changes to the application and need to set up a local development environment, please read on.

### Run application server locally

There are two parts of the application: client-side and server-side. You can run them separately, this will
let you make changes to the client code and see the effect in your browser immediately. The production build currently 
bundles them together into a single JAR.

The client side can be run using `npm`:
- to initialize node modules, run `npm install` in the `./client` directory. You only need to do this once.
- to run the client and serve the static content, run `npm run start`. In IntelliJ IDEA you can do this by clicking
on a corresponding `Run` icon in `./client/package.json` file.

The server side can be run using the gradle `run` task. The application uses Postgres database and will attempt to
create a corresponding local container on start. You need to have Docker running locally for that step to succeed.

Note:

- your static content is served by default on the port 3000
- your server responds at the port 8081
- webpack development server forwards HTTP requests made from the web-browser to 8081 port, see `client/src/setupProxy.js`

This means you can access both client side (static content) and server side of the application at http://localhost:3000.

### Make application server available to Space and Slack

You need to expose your locally running server to the internet. One way to do this is `ngrok`. After it is installed
locally, please run:

```shell
ngrok http 3000 --host-header="localhost:3000"
```

Note the `host-header` parameter. It is required for the proxy in webpack dev server to function properly.

You can now use the public `ngrok` url as a hostname for your application.