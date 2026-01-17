# Gradle Plugins Collection

[![Build and Test](https://github.com/Lekanich/GradlePlugins/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/Lekanich/GradlePlugins/actions/workflows/build-and-test.yml)

A collection of Gradle plugins for build automation and tooling. Each plugin is independently developed, tested, and published to provide focused functionality for your build process.

## Plugins

### 🔧 Git Tool Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.lekanich.git-tool?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.lekanich.git-tool)
![Code Coverage](https://img.shields.io/badge/coverage-93.7%25-brightgreen)

**Main Purpose:** A comprehensive Gradle plugin for automating Git operations in your build process.

**Installation:**
```kotlin
plugins {
    id("io.github.lekanich.git-tool") version "1.1.0"
}
```

**Documentation:** See [git-tool/README.md](git-tool/README.md) for detailed usage instructions.

**Version:** 1.1.0

## Project Structure

- Each plugin is located in its own subdirectory (e.g., `git-tool/`)
- Plugins use Gradle composite builds approach
- All plugins are independently publishable to Gradle Plugin Portal
- Common dependencies managed via `gradle/libs.versions.toml`

## Author

**Oleksandr Zhelezniak** ([@Lekanich](https://github.com/Lekanich))
