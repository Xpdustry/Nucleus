plugins {
    id("nucleus.base-conventions")
    id("com.github.johnrengelman.shadow")
}

project.version = rootProject.version

dependencies {
    implementation(projects.nucleusCore)

    // Javacord
    implementation(libs.javacord.api)
    implementation(libs.emoji)
    runtimeOnly(libs.javacord.core)
    runtimeOnly(libs.slf4j.simple)
    runtimeOnly(libs.log4j.to.slf4j) // Javacord uses log4j
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

tasks.shadowJar {
    archiveFileName.set("NucleusDiscord.jar")
    manifest {
        attributes(
            "Main-Class" to "fr.xpdustry.nucleus.discord.NucleusBotLauncher",
            "Implementation-Title" to "NucleusDiscord",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Xpdustry"
        )
    }
    from(rootProject.file("VERSION.txt"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register<JavaExec>("runNucleusDiscord") {
    dependsOn(tasks.shadowJar)
    classpath(tasks.shadowJar)
    group = "nucleus"
    description = "Runs NucleusDiscord"
    mainClass.set("fr.xpdustry.nucleus.discord.NucleusBotLauncher")
}
