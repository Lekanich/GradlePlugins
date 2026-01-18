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
        register("git-statistic") {
            id = "io.github.lekanich.git-statistic"
            displayName = "Lekanich's Gradle plugin for Git statistics and reporting"
            description = "A Gradle plugin for generating Git statistics, status reports, and dashboards"
            tags = listOf("lekanich", "git", "statistics", "reporting", "dashboard")
            implementationClass = "lekanich.common.gradle.statistic.GitStatisticPlugin"
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
                    name.set("Gradle :: Plugin :: Git Statistic")
                    description.set("A Gradle plugin for Git statistics and reporting")
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
