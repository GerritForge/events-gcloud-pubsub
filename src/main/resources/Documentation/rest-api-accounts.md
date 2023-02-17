@PLUGIN@ - /accounts/ REST API
==============================

This page describes the REST endpoints that are added by the @PLUGIN@.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="account-endpoints"> Account Endpoints
--------------------------------------------

### <a id="create-topic"> Create Topic
_PUT /accounts/[\{account-id\}](../../../Documentation/rest-api-accounts.html#account-id)/pubsub.topic_

Creates a PubSub topic for the account. Only a single topic can be
created per account.

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


GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
