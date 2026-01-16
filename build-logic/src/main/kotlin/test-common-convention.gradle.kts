/**
 * Reusable build script for submodules that provides:
 * - JaCoCo test coverage configuration
 * - JUnit 5 test dependencies and configuration
 */

plugins {
    jacoco
    java
    `java-library`
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(libs.findBundle("junit.jupiter").get())
    testRuntimeOnly(libs.findBundle("junit.jupiter.platform").get())
}

tasks {
    test {
        useJUnitPlatform()

        finalizedBy(jacocoTestReport)

        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }

    withType<Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    jacocoTestReport {
        dependsOn(test)

        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = "0.5".toBigDecimal()
                }
            }
        }
    }
}
