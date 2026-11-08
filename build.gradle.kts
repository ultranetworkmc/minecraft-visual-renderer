plugins {
    `java-library`
    `maven-publish`
}

group = "net.ultranetwork"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Provided at runtime by Paper (Adventure, Gson). Not shaded on purpose.
    compileOnly(libs.paper.api)

    testImplementation(libs.paper.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    // e.g. -Pmvr.updateGolden=true, -Pmvr.assets.version=26.3 -Pmvr.assets.cache=/path
    providers.gradlePropertiesPrefixedBy("mvr.").get().forEach { (key, value) -> systemProperty(key, value) }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Minecraft Visual Renderer")
                description.set("Render vanilla-accurate Minecraft tooltips and tab lists to images")
            }
        }
    }
}
