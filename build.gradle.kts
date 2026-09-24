plugins {
    java
    id("dev.architectury.loom") version "1.2-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
}

group = property("maven_group")!!
version = property("mod_version")!!
base { archivesName.set(property("archives_name").toString()) }

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    silentMojangMappingsLicense()
}

repositories {
    mavenCentral()
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    minecraft("net.minecraft:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    add("forge", "net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    modImplementation("com.cobblemon:forge:${property("cobblemon_version")}")
    implementation("thedarkcolour:kotlinforforge:4.12.0")
    modImplementation("maven.modrinth:tough-as-nails:kWVOI0iS")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}
