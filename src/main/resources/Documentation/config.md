Configuration
=========================

The gcloud-pubsub-events plugin is configured by adding a plugin stanza in the
`gerrit.config` file, for example:

```text
[plugin "gcloud-pubsub-events"]
    numberOfSubscribers = 6
    subscriptionId = instance-1
    gcloudProject = test_project
    privateKeyLocation = /var/gerrit/etc/secured_key.json

```

`plugin.gcloud-pubsub-events.gcloudProject`
:   GCloud [project name](https://cloud.google.com/docs/overview#projects)

`plugin.gcloud-pubsub-events.subscriptionId`
:   Conditional. This value identifies the subscriber and it must be unique within your
    Gerrit cluster to allow different Gerrit nodes to consume data from the
    stream independently. It can be omitted when `gerrit.instanceId` is
    configured, otherwise it is mandatory.
    Default: `gerrit.instanceId` value (when defined)
    See also: [gerrit.instanceId](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#gerrit.instanceId)

`plugin.gcloud-pubsub-events.privateKeyLocation`
:   Path to the JSON file that contains service account key. The file
    should be readable only by the daemon process because it contains information
    that wouldn’t normally be exposed to everyone.

`plugin.gcloud-pubsub-events.numberOfSubscribers`
:   Optional. The number of expected gcloud-pubsub-events subscribers. This will be used
    to allocate a thread pool able to run all subscribers.
    Default: 6

`plugin.gcloud-pubsub-events.sendAsync`
:   Optional. Send messages to GCloud PubSub asynchronously, detaching the calling
    process from the acknowledge of the message being sent.
    The drawback of the enabling the sendAsync parameter is that the broker would only
    return the status of the successful invocation of the message send operation and not
    the actual status received by the broker. This means that when sendAsync is enabled
    'broker_msg_publisher_failure_counter' metric is not incremented when message send
    failure occurs.
    Default: true

`plugin.gcloud-pubsub-events.ackDeadlineSeconds`
:   Optional. The approximate amount of time (on a best-effort basis) Pub/Sub waits for
    the subscriber to acknowledge receipt before resending the message.
    Default: 10

`plugin.gcloud-pubsub-events.subscribtionTimeoutInSeconds`
:   Optional. Maximum time in seconds to wait for the subscriber to connect to GCloud PubSub topic.
    Default: 10

`plugin.gcloud-pubsub-events.streamEventsTopic`
:   Optional. Name of the GCloud PubSub topic for stream events. GCloud-pubsub-events plugin exposes
    all stream events under this topic name.
    Default: gerrit
