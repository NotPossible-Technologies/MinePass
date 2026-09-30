plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "io.github.duckycashy"
version = "1.0.0"

val minecraftVersion = "26.2"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(
        "io.papermc.paper:paper-api:${minecraftVersion}.build.+"
    )
}

java {
    toolchain {
        languageVersion.set(
            JavaLanguageVersion.of(25)
        )
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {
        val properties = mapOf(
            "version" to project.version
        )

        inputs.properties(properties)

        filesMatching("plugin.yml") {
            expand(properties)
        }
    }

    runServer {
        minecraftVersion(minecraftVersion)
    }

    jar {
        archiveBaseName.set("MinePass")
        archiveVersion.set(
            "${project.version}_${minecraftVersion}"
        )
    }
}