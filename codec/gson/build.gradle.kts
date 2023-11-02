plugins {
    id("com.alexsobiek.jws.java-conventions")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.netty)
    implementation(libs.gson)
}