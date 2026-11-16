// Gemeinsame Einstellungen für alle Java-Module: Java 21, Spring-Boot-BOM, JUnit 5.
plugins {
    `java-library`
}

group = "de.firma.kuze"
version = "0.1.0-SNAPSHOT"

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val bom = platform(libs.findLibrary("spring-boot-dependencies").get())
    api(bom) // api statt implementation: so gilt die BOM auch für testFixtures und abhängige Module
    annotationProcessor(bom)
    testImplementation(bom)

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
