plugins {
    `kotlin-dsl`
    alias(libs.plugins.plugin.publish)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.junit.jupiter)
    testRuntimeOnly(libs.bundles.junit.jupiter.platform)
}

kotlin {
    jvmToolchain(17)
}

group = "io.github.lekanich.tool.gradle"
version = providers.gradleProperty("version").get()

gradlePlugin {
    website = "https://github.com/Lekanich/GradlePlugins"
    vcsUrl = "https://github.com/Lekanich/GradlePlugins.git"
    plugins {
        register("git-tool") {
            id = "io.github.lekanich.git-tool"
            displayName = "Lekanich's Gradle plugin for Git operations"
            description = "A Simple Gradle plugin for Git operations"
            tags = listOf("lekanich")
            implementationClass = "lekanich.common.gradle.GitToolPlugin"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()

        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("pluginMaven") {
                pom {
                    name.set("Gradle :: Plugin :: Git Tool")
                    description.set("A Gradle plugin for Git operations")
                    url.set("https://github.com/Lekanich/GradlePlugins")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("lekanich")
                            name.set("Oleksandr Zhelezniak")
                            email.set("lekan1992@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/Lekanich/GradlePlugins.git")
                        developerConnection.set("scm:git:ssh://github.com/Lekanich/GradlePlugins.git")
                        url.set("https://github.com/Lekanich/GradlePlugins")
                    }
                }
            }
        }
    }
}