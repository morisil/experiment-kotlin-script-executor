import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

group = "com.xemantic.script"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.xemantic.script.executor.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("script-executor.jar")
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)

    implementation(libs.kotlin.scripting.jsr223)
    implementation(libs.graalvm.polyglot)
    implementation(libs.graalvm.js)

    testImplementation(libs.kotlin.test)
}
