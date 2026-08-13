Configuration
=========================

The events-gcloud-pubsub plugin is configured by adding a plugin stanza in the
`gerrit.config` file, for example:

```text
[plugin "events-gcloud-pubsub"]
    numberOfSubscribers = 6
    subscriptionId = instance-1
    gcloudProject = test_project
    privateKeyLocation = /var/gerrit/etc/secured_key.json

```

`plugin.events-gcloud-pubsub.gcloudProject`
:   GCloud [project name](https://cloud.google.com/docs/overview#projects)

`plugin.events-gcloud-pubsub.subscriptionId`
:   Conditional. This value identifies the subscriber and it must be unique within your
    Gerrit cluster to allow different Gerrit nodes to consume data from the
    stream independently. It can be omitted when `gerrit.instanceId` is
    configured, otherwise it is mandatory.
    Default: `gerrit.instanceId` value (when defined)
    See also: [gerrit.instanceId](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#gerrit.instanceId)

`plugin.events-gcloud-pubsub.privateKeyLocation`
:   Path to the JSON file that contains service account key. The file
    should be readable only by the daemon process because it contains information
    that wouldn’t normally be exposed to everyone.

`plugin.events-gcloud-pubsub.numberOfSubscribers`
:   Optional. The number of expected events-gcloud-pubsub subscribers. This will be used
    to allocate a thread pool able to run all subscribers.
    Default: 6

`plugin.events-gcloud-pubsub.ackDeadlineSeconds`
:   Optional. The approximate amount of time (on a best-effort basis) Pub/Sub waits for
    the subscriber to acknowledge receipt before resending the message.
    Default: 10

`plugin.events-gcloud-pubsub.enableAutoCommit`
:   Optional. Whether the plugin should acknowledge consumed messages automatically.
    When set to `false`, Pub/Sub messages are acknowledged only when `ack(event)` is invoked.
    Default: true

`plugin.events-gcloud-pubsub.subscribtionTimeoutInSeconds`
:   Optional. Maximum time in seconds to wait for the subscriber to connect to GCloud PubSub topic.
    Default: 10

`plugin.events-gcloud-pubsub.streamEventsTopic`
:   Optional. Name of the GCloud PubSub topic for stream events. events-gcloud-pubsub plugin exposes
    all stream events under this topic name.
    Default: gerrit

`plugin.events-gcloud-pubsub.sendStreamEvents`
:   Whether to send stream events to the `streamEventsTopic` topic.
    Default: false

Logical partitions
------------------

Logical partition configuration and its shared behaviour are documented by
[events-broker](https://gerrit.googlesource.com/modules/events-broker/+/refs/heads/master/README.md#partition_aware-topics).

Messages are still published to the same Pub/Sub topic with the same JSON
payload. For a partition-aware topic, events-gcloud-pubsub copies the configured
partition event property and its value to a Pub/Sub message attribute. For
example, an event routed by `type=change-index` has the Pub/Sub attribute
`type=change-index`. Pub/Sub subscriptions can then use that attribute in a
filter.

Topics without configured partition values keep the existing behaviour and
are published without a partition attribute.

A partition-aware consumer uses a Pub/Sub subscription filter that matches the
corresponding message attribute, for example
`attributes.type = "change-index"`. The subscription keeps the existing
`<groupId>-<topic>` naming. Callers must therefore use a different group ID for
each logical partition.

Pub/Sub subscriptions persist independently of the plugin, and their filters
cannot be changed after creation. If a same-named subscription already exists
without the expected filter, or with a different one, the consumer is not
started. Use a new group ID or recreate the subscription with the expected
filter.

Gerrit init integration
-----------------------

The @PLUGIN@ plugin provides an init step that helps to set up the configuration.

```
*** events-gcloud-pubsub plugin
***

Should send stream events?     [y/N]? y
Stream events topic            [gerrit]:
Number of subscribers          [6]:
Timeout for subscriber ACKs (secs) [10]:
Should acknowledge messages automatically? [Y/n]?
Timeout for subscriber connection (secs) [10]:
Timeout for subscriber shutdown (secs) [10]:
Gcloud Project name            : some_project
Subscriber Id                  [6f174800-b1fa-477f-af49-26734f433280]:
Private key location           : /path/ssh/id_rsa
```
