package de.firma.kuze.sapclient

import org.gradle.api.provider.Property

/**
 * Konfiguration für das Plugin "kuze.sap-client".
 *
 * Eigener Dateityp statt Nested-Class im Skript, damit der Typ auch außerhalb des
 * Skripts (z. B. aus dem zentralen build.gradle.kts) referenzierbar ist.
 */
interface SapClientExtension {
    /** Dateiname der Teil-Spec, z. B. "common.yaml". */
    val teilSpec: Property<String>

    /** Basis-Package des Clients; API-Klassen landen in ".api", Modelle in ".model". */
    val paket: Property<String>
}
