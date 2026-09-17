plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.3.2"
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.purpurmc.org/snapshots")
    maven("https://jitpack.io")
}

val slimefunLegacyJar = file("libs/Slimefun-Legacy.jar")
val platform = providers.gradleProperty("platform").orElse("paper").get().lowercase()
val platformVersion = providers.gradleProperty("platformVersion").orElse(
    if (platform == "folia") "26.2.build.+" else "1.21.11-R0.1-SNAPSHOT"
).get()
val releaseJvm = providers.gradleProperty("releaseJvm").orElse("21").get().toInt()
val platformApi = when (platform) {
    "paper" -> "io.papermc.paper:paper-api:$platformVersion"
    "purpur" -> "org.purpurmc.purpur:purpur-api:$platformVersion"
    "folia" -> "dev.folia:folia-api:$platformVersion"
    else -> error("Unsupported platform '$platform'. Use paper, purpur, or folia.")
}

dependencies {
    compileOnly(platformApi)

    if (slimefunLegacyJar.exists()) {
        compileOnly(files(slimefunLegacyJar))
    } else {
        compileOnly("com.github.Slimefun:Slimefun4:RC-37")
    }
}

group = "io.github.wickidcow"
version = "0.1.0"
description = "Clean-room DracFun compatibility addon for Slimefun Legacy"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.toVersion(releaseJvm)
    targetCompatibility = JavaVersion.toVersion(releaseJvm)
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(releaseJvm)
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("SF_DracFun2${project.version}.jar")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

bukkit {
    main = "io.github.wickidcow.sfdracfun2.SFDracFun2"
    apiVersion = "1.21.11"
    foliaSupported = true
    authors = listOf("wickidcow")
    description = "Clean-room Slimefun Legacy reimplementation of discontinued DracFun gameplay systems"
    website = "https://github.com/wickidcow/SF_DracFun2"
    depend = listOf("Slimefun")
}
