plugins {
    java
}

group = "io.github.lthewiredlabs"
version = "1.2"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    testImplementation("io.papermc.paper:paper-api:26.2.build.121-stable")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveFileName.set("NetherChance-${project.version}.jar")
}

// Verification uses executable assertion suites instead of a JUnit engine.
tasks.test {
    enabled = false
}

val policyTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs the dependency-free Nether Chance policy tests."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("io.github.lthewiredlabs.netherchance.NetherChancePolicyTest")
    enableAssertions = true
}

val phraseTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks phrase rotation, configuration fallback, and remembered selections."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("io.github.lthewiredlabs.netherchance.NetherPhrasesTest")
    enableAssertions = true
}

val atmosphereTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks weather timing, world selection, cosmetic lightning, and sound delivery."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("io.github.lthewiredlabs.netherchance.NetherAtmosphereTest")
    enableAssertions = true
}

tasks.check {
    dependsOn(policyTest, phraseTest, atmosphereTest)
}
