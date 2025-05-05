plugins {
    id("java")
}

group = "net.ultranetwork"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}