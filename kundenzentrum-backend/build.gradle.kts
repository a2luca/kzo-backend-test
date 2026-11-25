import de.firma.kuze.sapclient.SapClientExtension
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

// Zentrale Build-Datei für alle Module. Modul-spezifische Konfiguration steht in den
// project(":modul") { ... }-Blöcken weiter unten; wiederverwendbare Build-Logik (Java-Konventionen,
// SAP-Client-Generierung) bleibt in buildSrc, weil sie einen eigenen Extension-Typ (SapClientExtension)
// und einen eigenen Task-Typ (GenerateTask) braucht.
val rootLibs = the<VersionCatalogsExtension>().named("libs")

// Gemeinsame Einstellungen für alle Module: Java 21, JUnit 5 (siehe buildSrc/.../kuze.java-conventions.gradle.kts).
// Für die SAP-Client-Module kommt das Plugin transitiv über "kuze.sap-client" mit; eine doppelte
// Anwendung ist unschädlich.
//
// Die Spring-Boot-BOM steht hier und nicht in der Convention-Plugin, weil Module ohne eigene
// build.gradle.kts keine Typesafe-Version-Catalog-Extension bekommen; der Katalog ist deshalb oben
// als "rootLibs" eingefangen statt in jedem Modul neu nachgeschlagen zu werden. Nicht "libs" nennen:
// das würde innerhalb der project(...)-Blöcke den (dort nicht vorhandenen) impliziten Projekt-Accessor
// gleichen Namens verdecken und zur Laufzeit fehlschlagen.
subprojects {
    apply(plugin = "kuze.java-conventions")

    dependencies {
        val bom = platform(rootLibs.findLibrary("spring-boot-dependencies").get())
        add("api", bom) // api statt implementation: so gilt die BOM auch für testFixtures und abhängige Module
        add("annotationProcessor", bom)
        add("testImplementation", bom)
    }
}

// Abhängigkeiten aus dem Versions-Katalog, die "kuze.sap-client" (buildSrc) selbst nicht mehr
// hinzufügen kann (siehe kuze.sap-client.gradle.kts), gemeinsam für alle drei SAP-Client-Module.
fun Project.sapClientKatalogAbhaengigkeiten() {
    dependencies {
        // Mapper zwischen generierten SAP-Klassen und den DTOs aus dem Core
        add("implementation", rootLibs.findLibrary("mapstruct").get())
        add("annotationProcessor", rootLibs.findLibrary("mapstruct-processor").get())
        add("testImplementation", rootLibs.findLibrary("wiremock").get())
    }
}

// Der Core hängt von keinem anderen Modul dieses Projekts ab, nur von Spring.
project(":core") {
    dependencies {
        add("api", "org.springframework.boot:spring-boot-starter-web")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
    }
}

project(":adapter-common") {
    apply(plugin = "kuze.sap-client")
    apply(plugin = "java-test-fixtures")
    sapClientKatalogAbhaengigkeiten()

    configure<SapClientExtension> {
        teilSpec = "common.yaml"
        paket = "de.firma.kuze.sap.common.client"
    }

    dependencies {
        add("api", project(":core"))
        // SapProperties, RestClient.Builder und @ConfigurationProperties
        add("implementation", "org.springframework.boot:spring-boot-autoconfigure")
    }
}

// Hängt von adapter-common ab (gemeinsamer RestClient, SapProperties), nie von adapter-odr.
project(":adapter-netze") {
    apply(plugin = "kuze.sap-client")
    sapClientKatalogAbhaengigkeiten()

    configure<SapClientExtension> {
        teilSpec = "netze-rest.yaml"
        paket = "de.firma.kuze.sap.netze.client"
    }

    dependencies {
        add("api", project(":core"))
        add("implementation", project(":adapter-common"))
        add("implementation", "org.springframework.boot:spring-boot-autoconfigure")
        add("testImplementation", testFixtures(project(":adapter-common")))
    }
}

// Hängt von adapter-common ab (gemeinsamer RestClient, SapProperties), nie von adapter-netze.
project(":adapter-odr") {
    apply(plugin = "kuze.sap-client")
    sapClientKatalogAbhaengigkeiten()

    configure<SapClientExtension> {
        teilSpec = "odr-rest.yaml"
        paket = "de.firma.kuze.sap.odr.client"
    }

    dependencies {
        add("api", project(":core"))
        add("implementation", project(":adapter-common"))
        add("implementation", "org.springframework.boot:spring-boot-autoconfigure")
        add("testImplementation", testFixtures(project(":adapter-common")))
    }
}

// Bindet alles zusammen. Beide Mandanten-Adapter sind immer im Artefakt; welche Beans aktiv sind,
// entscheidet das Profil beim Start.
project(":app") {
    apply(plugin = "org.springframework.boot")

    dependencies {
        add("implementation", project(":core"))
        add("implementation", project(":adapter-common"))
        add("implementation", project(":adapter-netze"))
        add("implementation", project(":adapter-odr"))

        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        add("testImplementation", rootLibs.findLibrary("wiremock").get())
        add("testImplementation", rootLibs.findLibrary("archunit").get())
    }

    tasks.named<Test>("test") {
        // Für KeinGenerierterCodeTest: durchsucht alle src/-Verzeichnisse des Projekts.
        val wurzel = rootProject.layout.projectDirectory.asFile.absolutePath
        systemProperty("projekt.verzeichnis", wurzel)
        inputs.files(fileTree(wurzel) { include("*/src/**") }).withPropertyName("quellen")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }

    tasks.named("jar") {
        enabled = false // nur das ausführbare bootJar
    }

    tasks.named<BootJar>("bootJar") {
        archiveFileName = "kundenzentrum-backend.jar"
    }

    // Start mit fest eingestelltem Mandanten, ohne Argumente: ./gradlew :app:bootRunNetze bzw. :app:bootRunOdr
    // (in VS Code: app → application → bootRunNetze / bootRunOdr).
    val hauptSourceSet = the<SourceSetContainer>()["main"]
    for (mandant in listOf("netze", "odr")) {
        tasks.register<BootRun>("bootRun" + mandant.replaceFirstChar { it.uppercase() }) {
            group = "application"
            description = "Startet das Backend für den Mandanten $mandant."
            classpath = hauptSourceSet.runtimeClasspath
            mainClass = "de.firma.kuze.KundenzentrumApplication"
            args("--spring.profiles.active=$mandant")
        }
    }
}

// Steht bewusst neben der Anwendung: hängt von keinem anderen Modul ab und wird nicht ausgeliefert.
project(":sap-mock") {
    apply(plugin = "application")

    dependencies {
        add("implementation", rootLibs.findLibrary("wiremock").get())
    }

    configure<JavaApplication> {
        mainClass = "de.firma.kuze.sapmock.SapMock"
    }
}

project(":spec-tools") {
    dependencies {
        add("implementation", rootLibs.findLibrary("swagger-parser").get())
        add("runtimeOnly", "org.slf4j:slf4j-nop")
    }

    val specVerzeichnis = rootProject.layout.projectDirectory.dir("specs/sap")

    tasks.named<Test>("test") {
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
    val hauptSourceSet = the<SourceSetContainer>()["main"]

    val specsAufteilen = tasks.register<JavaExec>("specsAufteilen") {
        group = "build"
        description = "Vergleicht netze.yaml und odr.yaml und schreibt common.yaml, netze-rest.yaml, odr-rest.yaml."
        val netzeYaml = specVerzeichnis.file("netze.yaml").asFile
        val odrYaml = specVerzeichnis.file("odr.yaml").asFile
        val ausgabe = teilSpecVerzeichnis.get().asFile
        inputs.files(netzeYaml, odrYaml).withPropertyName("specs").withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir(ausgabe).withPropertyName("ausgabe")
        outputs.cacheIf { true }
        classpath = hauptSourceSet.runtimeClasspath
        mainClass = "de.firma.kuze.spectools.SpecVergleichMain"
        args(netzeYaml.absolutePath, odrYaml.absolutePath, ausgabe.absolutePath)
    }

    configurations.consumable("teilSpecs")

    artifacts {
        add("teilSpecs", teilSpecVerzeichnis) {
            builtBy(specsAufteilen)
        }
    }
}
