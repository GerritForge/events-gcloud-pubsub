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

Creates a PubSub topic for the account. Users that do not have the
`Maintain Server` capability can only create topics for themselves.
Only a single topic can be created per account.

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

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
