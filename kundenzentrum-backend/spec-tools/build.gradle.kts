plugins {
    id("kuze.java-conventions")
}

dependencies {
    implementation(libs.swagger.parser)
    runtimeOnly("org.slf4j:slf4j-nop")
}

val specVerzeichnis = rootProject.layout.projectDirectory.dir("specs/sap")

tasks.test {
    // Ohne diese Inputs hielte Gradle den Bericht-Test nach einer Spec-Änderung für aktuell.
    inputs.dir(specVerzeichnis).withPropertyName("specs").withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("spec.verzeichnis", specVerzeichnis.asFile.absolutePath)
    systemProperty(
        "spec.bericht.aktualisieren",
        providers.systemProperty("spec.bericht.aktualisieren").getOrElse("false"),
    )
}

// Teilt die beiden SAP-Specs auf. Die Adapter-Module holen sich das Ergebnis über die
// Konfiguration "teilSpecs" (siehe kuze.sap-client.gradle.kts), dadurch kennt Gradle die
// Reihenfolge auch dann, wenn nur ein einzelnes Adapter-Modul gebaut wird.
val teilSpecVerzeichnis = layout.buildDirectory.dir("specs")

val specsAufteilen = tasks.register<JavaExec>("specsAufteilen") {
    group = "build"
    description = "Vergleicht netze.yaml und odr.yaml und schreibt common.yaml, netze-rest.yaml, odr-rest.yaml."
    val netzeYaml = specVerzeichnis.file("netze.yaml").asFile
    val odrYaml = specVerzeichnis.file("odr.yaml").asFile
    val ausgabe = teilSpecVerzeichnis.get().asFile
    inputs.files(netzeYaml, odrYaml).withPropertyName("specs").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(ausgabe).withPropertyName("ausgabe")
    outputs.cacheIf { true }
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "de.firma.kuze.spectools.SpecVergleichMain"
    args(netzeYaml.absolutePath, odrYaml.absolutePath, ausgabe.absolutePath)
}

configurations.consumable("teilSpecs")

artifacts {
    add("teilSpecs", teilSpecVerzeichnis) {
        builtBy(specsAufteilen)
    }
}
