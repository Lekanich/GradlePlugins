fun properties(key: String): Provider<String> = providers.gradleProperty(key)

plugins {
    java
    alias(libs.plugins.kotlin) // Kotlin support
}

group = "lekanich"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform(libs.junit))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks {
    test {
        useJUnitPlatform()
    }
    wrapper { gradleVersion = properties("gradleVersion").get() }

}