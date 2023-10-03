# Build

The events-gcloud-pubsub plugin can be built as a regular 'in-tree' plugin. That means
that is required to clone a Gerrit source tree first and then to have the plugin
source directory into the `/plugins` path. The plugin depends on [events-broker](https://gerrit.googlesource.com/modules/events-broker)
which is linked directly from into the `modules` folder.

Additionally, the `plugins/external_plugin_deps.bzl` file needs to be updated to
match the events-gcloud-pubsub plugin one.

```shell script
git clone --recursive https://gerrit.googlesource.com/gerrit
cd gerrit
git clone "https://gerrit.googlesource.com/plugins/events-gcloud-pubsub" plugins/events-gcloud-pubsub
git clone "https://gerrit.googlesource.com/modules/events-broker" modules/events-broker
ln -sf ../plugins/events-gcloud-pubsub/external_plugin_deps.bzl plugins/.
bazelisk build plugins/events-gcloud-pubsub
```

The output is created in

```
bazel-bin/plugins/events-gcloud-pubsub/events-gcloud-pubsub.jar
```

This project can be imported into the Eclipse IDE.
Add the plugin name to the `CUSTOM_PLUGINS` set in
Gerrit core in `tools/bzl/plugins.bzl`, and execute:

```
  ./tools/eclipse/project.py
```

To execute the tests run either one of:

```
  bazelisk test --test_tag_filters=@PLUGIN@ //...
  bazelisk test plugins/@PLUGIN@:@PLUGIN@_tests
```
Tests prerequisite:
* Docker

How to build the Gerrit Plugin API is described in the [Gerrit
documentation](../../../Documentation/dev-bazel.html#_extension_and_plugin_api_jar_files).
