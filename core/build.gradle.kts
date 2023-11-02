plugins {
    id("com.alexsobiek.jws.java-conventions")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.netty)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    shadowJar {
        minimize()
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}
