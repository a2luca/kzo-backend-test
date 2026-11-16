import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("kuze.java-conventions")
    id("org.springframework.boot")
}

// Bindet alles zusammen. Beide Mandanten-Adapter sind immer im Artefakt; welche Beans aktiv sind,
// entscheidet das Profil beim Start.
dependencies {
    implementation(project(":core"))
    implementation(project(":adapter-common"))
    implementation(project(":adapter-netze"))
    implementation(project(":adapter-odr"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.wiremock)
    testImplementation(libs.archunit)
}

tasks.test {
    // Für KeinGenerierterCodeTest: durchsucht alle src/-Verzeichnisse des Projekts.
    val wurzel = rootProject.layout.projectDirectory.asFile.absolutePath
    systemProperty("projekt.verzeichnis", wurzel)
    inputs.files(fileTree(wurzel) { include("*/src/**") }).withPropertyName("quellen")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.jar {
    enabled = false // nur das ausführbare bootJar
}

tasks.bootJar {
    archiveFileName = "kundenzentrum-backend.jar"
}

// Start mit fest eingestelltem Mandanten, ohne Argumente: ./gradlew :app:bootRunNetze bzw. :app:bootRunOdr
// (in VS Code: app → application → bootRunNetze / bootRunOdr).
for (mandant in listOf("netze", "odr")) {
    tasks.register<BootRun>("bootRun" + mandant.replaceFirstChar { it.uppercase() }) {
        group = "application"
        description = "Startet das Backend für den Mandanten $mandant."
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "de.firma.kuze.KundenzentrumApplication"
        args("--spring.profiles.active=$mandant")
    }
}
