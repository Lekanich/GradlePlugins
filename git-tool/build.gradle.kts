plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

group = providers.gradleProperty("groupId").get()
version = providers.gradleProperty("version").get()

gradlePlugin {
    plugins {
        create("git-tool") {
            id = "lekanich.git-tool"
            implementationClass = "lekanich.common.gradle.GitToolPlugin"
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
                    groupId = group.toString()
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