plugins {
    id("nucleus.base-conventions")
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
}

dependencies {
    api(project(":nucleus-common"))
    implementation("org.javacord:javacord:3.7.0")
    runtimeOnly("org.apache.logging.log4j:log4j-to-slf4j:2.19.0")
    implementation("cloud.commandframework:cloud-core:${Versions.cloud}")
    implementation("cloud.commandframework:cloud-annotations:${Versions.cloud}")
    implementation("cloud.commandframework:cloud-javacord:${Versions.cloud}")
    implementation("org.aeonbits.owner:owner-java8:${Versions.owner}")
    implementation("fr.xpdustry:javelin-core:${Versions.javelin}")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("info.picocli:picocli-spring-boot-starter:4.6.3")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.bootJar.get().archiveFile.get().toString()) }
}
