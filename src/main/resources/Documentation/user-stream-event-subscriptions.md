# Setting up stream events for end users

Gerrit provides streaming of events via SSH for end users. This is useful for
triggering CI jobs based on events like ref-updated. This method is however
prone to lost events, e.g. if the CI system is offline for a while. Using a
message broker reduces the probability of this to happen.

While this plugin mainly provides a method for multiple Gerrit instances to
exchange events with each other, it also provides a way for users to subscribe
to events published to Google PubSub.

## Prerequisites

Following prerequisites have to be met (Prerequisites for sharing events between
Gerrit instances will be omitted here):

- A Google Cloud project
- A GCP service account with the following roles:
  - Pub/Sub Editor
  - Service Account Token Creator
  - Service Account User
- A service account key of the above user in JSON format

## Configuration

In the `gerrit.config` configure the following options
(see [config.md](./config.md#configuration) for more information):

- `plugin.events-gcloud-pubsub.gcloudProject`
- `plugin.events-gcloud-pubsub.privateKeyLocation`
- `plugin.events-gcloud-pubsub.enableUserStreamEvents`: \
    Has to be set to `true` to enable this feature.
- `plugin.events-gcloud-pubsub.serviceAccountForUserSubs`: \
    The service account created as described in the [prerequisites](#prerequisites)

## Permissions

Users who want to subscribe to events require the `Subscribe to stream events in PubSub`
global capability.

## Subscribing to events

To subscribe to events, the user has to create a PubSub topic for their account.
Each Gerrit account can only create a single topic. A user can only create a topic
for their own account with the exception of administrators who can create topics
for all accounts.

The topic can be created using the REST API:

```sh
curl -XPUT \
    --user johndoe \
    https://gerrit.example.com/a/accounts/self/pubsub.topic
```

Gerrit will automatically start publishing all events that are visible to that
account to the topic. While Gerrit will not persist, the topics associated to an
account, it will use a naming scheme uniquely identifying a topic of an account:
`stream-events-$SERVER_ID-$ACCOUNT_ID`. On startup, Gerrit will look up all topics
following this pattern in the configured GCP project and will start publishing
events to it.

For clients to receive events published to the account's topic, the user can
create subscriptions using the Gerrit REST API:

```sh
curl -XPUT \
    --user johndoe \
    https://gerrit.example.com/a/accounts/self/pubsub.sub \
    -H "Content-Type: application/json" \
    --data \
        '{"push_endpoint": "https://jenkins.example.com/events", \
          "verification_token": "1234token", \
          "internal": false}'
```

This will create a [push subscription](https://cloud.google.com/pubsub/docs/push)
in Google PubSub and attach it to the account's topic. PubSub will push events
to the endpoint configured in the creation request:

(following the example above): `https://jenkins.example.com/events?token=1234token`

The token sent as a parameter is a first way to verify the requests authenticity.
PubSub will further send a JWT-token in the authentication header, which should
be [validated](https://cloud.google.com/pubsub/docs/push#validate_tokens). The
audience and service account email used in the JWT-token will be returned by
Gerrit, when creating the subscription.

### Subscribing with clients in private networks

PubSub push subscriptions do not support pushing to private endpoints. However,
it is possible to proxy pushes using Google CloudRun and Serverless VPC Access,
if the private network is connected to a VPC in Google Cloud via Cloud Interconnect.

#### Setting up Serverless VPC Access

Set up a Serverless VPC Access using a SubNet in the VPC connected to the target
private network following Google's
[instructions](https://cloud.google.com/vpc/docs/configure-serverless-vpc-access).

#### Setting up CloudRun

The sources for a docker container that can proxy push requests are provided
under `/supplements/push-proxy`. Build the container and push it to the GCR
repository in the same GCP project used for PubSub.

Create a CloudRun Service:

1) Select the container image described above.
2) The region should be the same as the one used by PubSub.
3) Only allow `Internal` access
4) Require authentication
5) Expand `Container, Networking, Security` options
6) `Container`:
   1) `Container Port`: `8080`
   2) `CPU`: `2`
   3) `Request timeout`: `600` (maximum acknowledgement timeout of PubSub)
   4) `Maximum requests per container`: This will be used by CloudRun to decide
      when to scale and should be optimized to reduce the queue in Gunicorn.
   5) `Environment Variables` (Used to configure the proxy server):
      1) `FORWARD_JWT`: Whether to forward the JWT token used by PubSub instead
         of creating a new one with proper audience. (Currently, has to be true.)
      2) `NAMESERVERS`: Comma-separated list of nameserver IPs. Required to
         use non-default nameservers in private network (default: `8.8.8.8,4.4.4.4`)
7) `Networking`:
   1) Select the previously created Serverless VPC Access as a `VPC`.
8) `Security`:
   1) Select the Service Account that is configured for
      `plugin.events-gcloud-pubsub.serviceAccountForUserSubs` in the `gerrit.config`.
9) Click `Create`

#### Configuring Gerrit

Set `plugin.events-gcloud-pubsub.userSubProxyEndpoint` in `gerrit.config` to the
URL of the CloudRun service.

#### Creating a subscription for an endpoint in an internal network

In the request to Gerrit asking for a subscription set `internal` to `true`:

```sh
curl -XPUT \
    --user johndoe \
    https://gerrit.example.com/a/accounts/self/pubsub.sub \
    -H "Content-Type: application/json" \
    --data \
        '{"push_endpoint": "https://jenkins.example.com/events", \
          "verification_token": "1234token", \
          "internal": true}'
```

Gerrit will then configure the subscription to use the CloudRun proxy.

## Unsubscribing

Subscriptions will be automatically deleted, if no messages are delivered for more
than 1 day.

Topics can be deleted using Gerrit's REST API:

```sh
curl -XDELETE \
    --user johndoe \
    https://gerrit.example.com/a/accounts/self/pubsub.topic
```


