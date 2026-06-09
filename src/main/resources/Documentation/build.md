# Build

The events-gcloud-pubsub plugin can be built as a regular 'in-tree' plugin. That
means that it is required to clone a Gerrit source tree first and then to have
the plugin source directory into the `/plugins` path. The plugin depends on
[events-broker](https://gerrit.googlesource.com/modules/events-broker), which is
linked directly from source with the same 'in-tree' plugin structure.

From Gerrit's source tree, link Gerrit's Bazel version file into the plugin
repository so standalone plugin commands use the same Bazel version as Gerrit.

```
  ln -sf `pwd`/.bazelversion plugins/events-gcloud-pubsub
```

Put the external dependency Bazel module fragment into the Gerrit `/plugins`
directory, replacing the existing empty one.

```
  cd gerrit/plugins
  ln -fs events-gcloud-pubsub/external_plugin_deps.MODULE.bazel .
```

Then issue

```
  bazelisk build plugins/events-gcloud-pubsub
```

in the root of Gerrit's source tree to build.

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

### Updating Bazel modules

When the plugin's Bazel module dependencies change, regenerate the Bazel module
lockfile to ensure all module versions are recorded and reproducible.

Example:

```bash
  ln -sf `pwd`/.bazelversion plugins/events-gcloud-pubsub
  cd plugins/events-gcloud-pubsub
  bazelisk mod deps --lockfile_mode=update
```

This updates `MODULE.bazel.lock` with the currently resolved module versions.

### Pinning external dependencies

When the plugin's external dependencies are updated, regenerate the dependency
lockfile to pin the new versions.

Example:

```bash
  ln -sf `pwd`/.bazelversion plugins/events-gcloud-pubsub
  cd plugins/events-gcloud-pubsub
  REPIN=1 bazelisk run @events-gcloud-pubsub_plugin_deps//:pin
```

This updates `events-gcloud-pubsub_plugin_deps.lock.json` with the latest pinned
dependency versions.

How to build the Gerrit Plugin API is described in the [Gerrit
documentation](../../../Documentation/dev-bazel.html#_extension_and_plugin_api_jar_files).
