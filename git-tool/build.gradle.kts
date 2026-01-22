plugins {
    `kotlin-dsl`
    alias(libs.plugins.plugin.publish)
    id("test-common-convention")
}

repositories {
    gradlePluginPortal()
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
            displayName = "Git Tool - Git Operations Automation"
            description = "Automate Git operations in Gradle builds: check repository status, create/manage/push tags, get Git info (branch, commit hash, tags), and orchestrate complete release workflows with built-in validation."
            tags = listOf("git", "scm", "vcs", "version-control", "tagging", "release", "automation", "ci-cd")
            implementationClass = "lekanich.common.gradle.GitToolPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/lekanich/GradlePlugins")
            credentials {
                username = System.getenv("GH_ACTOR")
                password = System.getenv("GH_TOKEN")
            }
        }
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