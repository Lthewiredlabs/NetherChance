plugins {
    java
}

group = "io.github.lthewiredlabs"
version = "1.1"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
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

val policyTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs the dependency-free Nether Chance policy tests."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("io.github.lthewiredlabs.netherchance.NetherChancePolicyTest")
    enableAssertions = true
}

tasks.check {
    dependsOn(policyTest)
}
