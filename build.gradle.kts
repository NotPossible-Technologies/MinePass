plugins {
    base
}

group = "io.github.duckycashy"

version = "1.0.0"

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()

        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }
}