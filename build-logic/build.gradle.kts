plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("io.github.gmazzo.gradle.testkit.jacoco:io.github.gmazzo.gradle.testkit.jacoco.gradle.plugin:1.0.5")
}
