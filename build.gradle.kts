plugins {
    id("java")
    id("maven-publish")
}

group = "net.ultranetwork"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Minecraft Visual Renderer")
                description.set("Generate minecraft like images using builders")
            }
        }
    }
}
tasks.test {
    useJUnitPlatform()
}