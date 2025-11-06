import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.github.re7r"
version = "1.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        phpstorm("2025.2")
        bundledPlugin("com.jetbrains.php")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "261.*"
        }
    }

    pluginVerification {
        ides {
            ide("PS", "2025.2") // PhpStorm
            ide("IU", "2025.2") // IntelliJ IDEA Ultimate
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    buildSearchableOptions {
        enabled = false
    }

    runIde {
        systemProperty("idea.config.path", "${projectDir}/.idea/app/config")
        systemProperty("idea.system.path", "${projectDir}/.idea/app/system")
    }
}
