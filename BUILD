load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)
load("@rules_java//java:defs.bzl", "java_library")

PLUGIN = "events-gcloud-pubsub"

EXT_DEPS = [
    "com.google.api.grpc:proto-google-cloud-pubsub-v1",
    "com.google.api:api-common",
    "com.google.api:gax",
    "com.google.api:gax-grpc",
    "com.google.auth:google-auth-library-oauth2-http",
    "com.google.cloud:google-cloud-pubsub",
    "io.grpc:grpc-api",
]

TEST_EXT_DEPS = EXT_DEPS + [
    "org.testcontainers:gcloud",
    "org.testcontainers:testcontainers",
]

gerrit_plugin(
    srcs = glob(["src/main/java/**/*.java"]),
    ext_deps = EXT_DEPS,
    manifest_entries = [
        "Gerrit-PluginName: events-gcloud-pubsub",
        "Gerrit-Module: com.gerritforge.gerrit.plugins.pubsub.Module",
        "Gerrit-HttpModule: com.gerritforge.gerrit.plugins.bsl.HttpModule",
        "Gerrit-InitStep: com.gerritforge.gerrit.plugins.pubsub.InitConfig",
        "Implementation-Title: Gerrit events listener to send events to an external GCloud PubSub broker",
        "Implementation-URL: https://github.com/GerritForge/events-gcloud-pubsub",
    ],
    plugin = PLUGIN,
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":events-broker-neverlink",
        ":gerrit-provided-neverlink",
        "//plugins/gerrit-bsl-license",
    ],
)

gerrit_plugin_tests(
    name = "events-gcloud-pubsub_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    ext_deps = TEST_EXT_DEPS,
    plugin = PLUGIN,
    tags = ["events-gcloud-pubsub"],
    deps = [
        ":gerrit-provided-neverlink",
        "//plugins/events-broker",
    ],
)

java_library(
    name = "events-broker-neverlink",
    neverlink = 1,
    exports = ["//plugins/events-broker"],
)

java_library(
    name = "gerrit-provided-neverlink",
    neverlink = 1,
    exports = [
        "//lib:gson",
        "//lib:protobuf",
        "//lib/httpcomponents:httpclient",
        "//lib/httpcomponents:httpcore",
    ],
)
