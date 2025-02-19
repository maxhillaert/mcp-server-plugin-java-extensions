import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import org.gradle.kotlin.dsl.intellijPlatform
import java.util.*
import org.jetbrains.intellij.platform.gradle.*
import org.jetbrains.intellij.platform.gradle.models.*
import org.jetbrains.intellij.platform.gradle.tasks.*
import org.jetbrains.intellij.platform.gradle.utils.settings

plugins {
    id("org.jetbrains.intellij.platform") version "2.2.0"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")

        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledPlugin("Git4Idea")
        bundledPlugin("com.intellij.java")  // ensure the Java plugin is available
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
}

intellijPlatform {
    pluginConfiguration {
        version = settings.extra["pluginVersion"] as String
        ideaVersion {
            sinceBuild.set("242")
            untilBuild.set("251.*")
        }
    }
    publishing {
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }
}

kotlin {
    jvmToolchain(17)
}

