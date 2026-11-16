// Für Adapter-Module: holt die Teil-Spec aus spec-tools und generiert daraus einen RestClient-basierten
// SAP-Client nach build/generated/sap-client. Generierter Code landet nie unter src/.
//
// Anwendung im Modul:
//   plugins { id("kuze.sap-client") }
//   sapClient { teilSpec = "common.yaml"; paket = "de.firma.kuze.sap.common.client" }
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("kuze.java-conventions")
    id("org.openapi.generator")
}

val libs = the<VersionCatalogsExtension>().named("libs")

interface SapClientExtension {
    /** Dateiname der Teil-Spec, z. B. "common.yaml". */
    val teilSpec: Property<String>

    /** Basis-Package des Clients; API-Klassen landen in ".api", Modelle in ".model". */
    val paket: Property<String>
}

val sapClient = extensions.create<SapClientExtension>("sapClient")

// Die Teil-Specs kommen als Artefakt aus spec-tools. Dadurch läuft der Spec-Vergleich immer vor dem
// Generator, auch wenn nur dieses eine Modul gebaut wird (z. B. ./gradlew :adapter-netze:build).
val teilSpecs = configurations.dependencyScope("teilSpecs")
val teilSpecsAufgeloest = configurations.resolvable("teilSpecsAufgeloest") {
    extendsFrom(teilSpecs.get())
}
dependencies {
    add(teilSpecs.name, project(mapOf("path" to ":spec-tools", "configuration" to "teilSpecs")))
}

val ausgabe = layout.buildDirectory.dir("generated/sap-client")

val sapClientGenerieren = tasks.register<GenerateTask>("sapClientGenerieren") {
    group = "build"
    description = "Generiert den SAP-Client aus der Teil-Spec."
    val specVerzeichnis = teilSpecsAufgeloest.map { it.singleFile }
    inputs.files(teilSpecsAufgeloest).withPropertyName("teilSpecs").withPathSensitivity(PathSensitivity.RELATIVE)
    inputSpec = layout.file(specVerzeichnis.zip(sapClient.teilSpec) { verzeichnis, datei -> verzeichnis.resolve(datei) })
    outputDir = ausgabe
    cleanupOutput = true

    generatorName = "java"
    library = "restclient"
    packageName = sapClient.paket
    invokerPackage = sapClient.paket
    apiPackage = sapClient.paket.map { "$it.api" }
    modelPackage = sapClient.paket.map { "$it.model" }
    modelNamePrefix = "Sap"

    generateApiTests = false
    generateModelTests = false
    generateApiDocumentation = false
    generateModelDocumentation = false

    configOptions = mapOf(
        "openApiNullable" to "false",
        "useJakartaEe" to "true",
        "dateLibrary" to "java8",
        "serializationLibrary" to "jackson",
        "annotationLibrary" to "none",
        "documentationProvider" to "none",
        "hideGenerationTimestamp" to "true",
        "sourceFolder" to "src/main/java",
    )
}

sourceSets.main {
    java.srcDir(sapClientGenerieren.flatMap { it.outputDir.dir("src/main/java") })
}

dependencies {
    // Laufzeitbedarf des generierten restclient-Codes
    api("org.springframework:spring-web")
    api("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("jakarta.annotation:jakarta.annotation-api")

    // Mapper zwischen generierten SAP-Klassen und den DTOs aus dem Core
    implementation(libs.findLibrary("mapstruct").get())
    annotationProcessor(libs.findLibrary("mapstruct-processor").get())

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.findLibrary("wiremock").get())
}

tasks.compileJava {
    // Vergessene Felder im Mapping sollen den Build brechen, nicht still null liefern.
    options.compilerArgs.add("-Amapstruct.unmappedTargetPolicy=ERROR")
}
