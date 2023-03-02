@PLUGIN@ - /accounts/ REST API
==============================

This page describes the REST endpoints that are added by the @PLUGIN@.
To subscribe to stream events via PubSub the `Subscribe to stream events in PubSub`
capability is required.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="account-endpoints"> Account Endpoints
--------------------------------------------

### <a id="create-topic"> Create Topic
_PUT /accounts/[\{account-id\}](../../../Documentation/rest-api-accounts.html#account-id)/pubsub.topic_

Creates a PubSub topic for the account.  Users that do not have the
`Maintain Server` capability can only create topics for themselves.
Only a single topic can be created per account. Gerrit will
automatically start to publish events visible to the account to
the topic.

When Gerrit starts, it will start to publish events to all already existing
topics.

Note, that Google PubSub limits the number of topics per project to
[10000](https://cloud.google.com/pubsub/quotas#resource_limits).

The created topic will have the name format
`stream-events-$GERRIT_SERVER_ID-$ACCOUNT_ID`. This ensures that the
topic can be associated to a server and account in that server.

Users will not be granted direct access to the topic by Gerrit. Changes
to the topic can only be performed via the Gerrit REST API.

#### Request

```
  PUT /accounts/self/pubsub.topic HTTP/1.0
```

#### Response

If the topic has been created:

```
  HTTP/1.1 201 Created
```


If the topic already existed:

```
  HTTP/1.1 204 No Content
```


### <a id="delete-topic"> Delete Topic
_DELETE /accounts/[\{account-id\}](../../../Documentation/rest-api-accounts.html#account-id)/pubsub.topic_

Deletes the PubSub topic for the account.

#### Request

```
  DELETE /accounts/self/pubsub.topic HTTP/1.0
```

#### Response

```
  HTTP/1.1 204 No Content
```

### <a id="create-subscription"> Create Subscription
_PUT /accounts/[\{account-id\}](../../../Documentation/rest-api-accounts.html#account-id)/pubsub.sub_

Creates a PubSub push subscription for the account and attaches it
to the account's PubSub topic. Multiple subscriptions can be created
per account to serve multiple subscribers.

#### Request

```
  PUT /accounts/self/pubsub.sub HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "push_endpoint": "https://example.com/events",
    "verification_token" "token1234"
  }
```

#### Response

```
  HTTP/1.1 201 Created
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "subscription_name": "projects/gcp-project/subscriptions/stream-events-1000000-cwyPaO",
    "audience": "admin",
    "verification_token": "token1234",
    "service_account_email": "serviceaccount@gcp-project.iam.gserviceaccount.com"
  }
```


<a id="json-entities">JSON Entities
-----------------------------------

### <a id="subscription-input"></a>SubscriptionInput

The `SubscriptionInput` entity contains parameters required for the
creation of a PubSub subscription.

* _push_endpoint_ : The URL PubSub should push the events to. Has to use HTTPS and publicly available.
* _verification_token_ : A string that will be send with the token parameter in the request to verify the request's origin.


### <a id="subscription-info"></a>SubscriptionInfo

The `SubscriptionInfo` entity contains metadata of the created
subscription.

* _subscription_name_ : Name of the created subscription.
* _audience_ : Audience used in the jwt-token sent by PubSub for authentication.
* _verification_token_ : A string that will be send with the token parameter in the request to verify the request's origin. Same as set in SubscriptionInput.
* _service_account_email_ : Email of the service account used by PubSub for the subscription. Part of the jwt token.


GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
