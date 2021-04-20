load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "junit-platform",
        artifact = "org.junit.platform:junit-platform-commons:1.4.0",
        sha1 = "34d9983705c953b97abb01e1cd04647f47272fe5",
    )

    maven_jar(
        name = "google-cloud-pubsub",
        artifact = "com.google.cloud:google-cloud-pubsub:1.111.4",
        sha1 = "01988db8241471b09fc317c803d20403d93d6ca5",
    )

    maven_jar(
        name = "google-cloud-pubsub-proto",
        artifact = "com.google.api.grpc:proto-google-cloud-pubsub-v1:1.93.4",
        sha1 = "167bfae34ec63215ee3b9e95a4deb0b67104c021",
    )

    maven_jar(
        name = "api-common",
        artifact = "com.google.api:api-common:1.10.1",
        sha1 = "d157681b5909cf959a9fa60ced9bed9da741ffef",
    )

    maven_jar(
        name = "google-auth-library-credentials",
        artifact = "com.google.auth:google-auth-library-credentials:0.24.1",
        sha1 = "5f43498fae558213e27cd904944626c88cf03d03",
    )

    maven_jar(
        name = "google-auth-library-oauth2-http",
        artifact = "com.google.auth:google-auth-library-oauth2-http:0.24.1",
        sha1 = "ce4ca632ff44eb9cb3033db590d4862a686db199",
    )

    maven_jar(
        name = "gax",
        artifact = "com.google.api:gax:1.62.0",
        sha1 = "11e565f1a65f7e2245238ac5c19875c0ddd25b14",
    )

    maven_jar(
        name = "events-broker",
        artifact = "com.gerritforge:events-broker:3.3.1",
        sha1 = "90775e671946b20e52be3a11277d1ed33973d66e",
    )

    maven_jar(
        name = "testcontainers-gcloud",
        artifact = "org.testcontainers:gcloud:1.15.2",
        sha1 = "0ad02bb83edc818469e1080995cae409f5d40694",
    )

    maven_jar(
        name = "grpc-api",
        artifact = "io.grpc:grpc-api:1.36.0",
        sha1 = "5a2c6286f76477a44aaf63c9f4d0f5399652885a",
    )

    maven_jar(
        name = "gax-grpc",
        artifact = "com.google.api:gax-grpc:1.62.0",
        sha1 = "9c8e22dbceac414c03bfc92abc4399e82208d647",
    )

    maven_jar(
        name = "grpc-core",
        artifact = "io.grpc:grpc-core:1.36.0",
        sha1 = "17a7f3287439c1d2641fabc4d767e7de4ebb05e5",
    )

    maven_jar(
        name = "grpc-netty-shaded",
        artifact = "io.grpc:grpc-netty-shaded:1.36.0",
        sha1 = "575d65cd47a8c997f2014e702920d23b9f18d764",
    )

    maven_jar(
        name = "threetenbp",
        artifact = "org.threeten:threetenbp:1.5.0",
        sha1 = "e18c0fb79ebee3e3907042a7f31c31404f9e546b",
    )

    maven_jar(
        name = "grpc-alts",
        artifact = "io.grpc:grpc-alts:1.36.0",
        sha1 = "e41ed1f9739daa26752885304855161e8d504fa0",
    )

    maven_jar(
        name = "grpc-protobuf",
        artifact = "io.grpc:grpc-protobuf:1.36.0",
        sha1 = "b579119c664bb1b50249c652ef139ef30adb68a9",
    )

    maven_jar(
        name = "grpc-protobuf-lite",
        artifact = "io.grpc:grpc-protobuf-lite:1.36.0",
        sha1 = "78f16d4544b8e83b845cd91ed9c24854f9f78a8c",
    )

    maven_jar(
        name = "proto-google-iam-v1",
        artifact = "com.google.api.grpc:proto-google-iam-v1:1.0.9",
        sha1 = "7a3ad48a2f9925ec8cd811dc06bca778ba9560cc",
    )

    maven_jar(
        name = "proto-google-common-protos",
        artifact = "com.google.api.grpc:proto-google-common-protos:2.1.0",
        sha1 = "65234da8719aed4672298f574d7e87c1f025c4dc",
    )

    maven_jar(
        name = "google-http-client",
        artifact = "com.google.http-client:google-http-client:1.39.0",
        sha1 = "936d09a3afa6911f1255ce039debeaf37928ee75",
    )

    maven_jar(
        name = "google-http-client-gson",
        artifact = "com.google.http-client:google-http-client-gson:1.39.0",
        sha1 = "24ec436b2b4a27c17ccfa83a9c885a1f582e29b8",
    )

    maven_jar(
        name = "grpc-context",
        artifact = "io.grpc:grpc-context:1.36.0",
        sha1 = "8d8c6e0f00ae7889f4a435c4ec0c4ad4cb99578d",
    )

    maven_jar(
        name = "grpc-stub",
        artifact = "io.grpc:grpc-stub:1.36.0",
        sha1 = "7154e1fbdc9a809f158e8990999760a1bab95ed7",
    )

    maven_jar(
        name = "perfmark-api",
        artifact = "io.perfmark:perfmark-api:0.23.0",
        sha1 = "0b813b7539fae6550541da8caafd6add86d4e22f",
    )

    maven_jar(
        name = "opencensus-api",
        artifact = "io.opencensus:opencensus-api:0.28.0",
        sha1 = "0fc0d06a9d975a38c581dff59b99cf31db78bd99",
    )

    maven_jar(
        name = "opencensus-contrib-http-util",
        artifact = "io.opencensus:opencensus-contrib-http-util:0.28.0",
        sha1 = "f6cb276330197d51dd65327fc305a3df7e622705",
    )

    maven_jar(
        name = "grpc-auth",
        artifact = "io.grpc:grpc-auth:1.36.0",
        sha1 = "d9722016658f8e649111c8bb93b299ea38dc207e",
    )