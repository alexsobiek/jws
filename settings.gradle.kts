pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}


rootProject.name = "jws"
include("core")
setupCodec("gson")

fun setupCodec(name: String) {
    setupSubproject("codec-$name") {
        projectDir = file("codec/$name");
    }
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
