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
    forge { mixinConfig("cobblemon_progression_bridge.mixins.json") }
}
repositories {
    mavenCentral()
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://maven.blamejared.com")
}
dependencies {
    minecraft("net.minecraft:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    add("forge", "net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    modImplementation("com.cobblemon:forge:${property("cobblemon_version")}")
    implementation("thedarkcolour:kotlinforforge:4.12.0")
    compileOnly("mezz.jei:jei-1.20.1-common-api:15.21.0.138")
    compileOnly("mezz.jei:jei-1.20.1-forge-api:15.21.0.138")
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/mods.toml") { expand("version" to project.version) }
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}
