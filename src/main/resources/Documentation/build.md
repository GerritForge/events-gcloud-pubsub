# Build

The gcloud-pubsub-events plugin can be built as a regular 'in-tree' plugin. That means
that is required to clone a Gerrit source tree first and then to have the plugin
source directory into the `/plugins` path.

Additionally, the `plugins/external_plugin_deps.bzl` file needs to be updated to
match the gcloud-pubsub-events plugin one.

```shell script
git clone --recursive https://gerrit.googlesource.com/gerrit
cd gerrit
git clone "https://review.gerrithub.io/GerritForge/gcloud-pubsub-events" plugins/gcloud-pubsub-events
ln -sf ../plugins/gcloud-pubsub-events/external_plugin_deps.bzl plugins/.
bazelisk build plugins/gcloud-pubsub-events
```

The output is created in

```
bazel-bin/plugins/gcloud-pubsub-events/gcloud-pubsub-events.jar
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
