package de.firma.kuze.spectools;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hält {@code specs/sap/vergleich.md} aktuell. Ändert eine neue SAP-Spec die Aufteilung, schlägt dieser
 * Test fehl. Bewusst übernehmen: {@code ./gradlew :spec-tools:test -Dspec.bericht.aktualisieren=true}
 */
class BerichtTest {

    @Test
    void eingecheckterBerichtIstAktuell() throws IOException {
        Path verzeichnis = Path.of(System.getProperty("spec.verzeichnis"));
        var ergebnis = SpecVergleich.vergleiche(
                Spec.laden("netze.yaml", verzeichnis.resolve("netze.yaml")),
                Spec.laden("odr.yaml", verzeichnis.resolve("odr.yaml")));
        String neu = Bericht.schreiben(ergebnis);
        Path datei = verzeichnis.resolve("vergleich.md");

        if (Boolean.getBoolean("spec.bericht.aktualisieren")) {
            Files.writeString(datei, neu);
            return;
        }

        assertThat(datei).as("specs/sap/vergleich.md fehlt").exists();
        assertThat(Files.readString(datei))
                .as("specs/sap/vergleich.md passt nicht mehr zu den Specs. Änderung prüfen und mit "
                        + "./gradlew :spec-tools:test -Dspec.bericht.aktualisieren=true übernehmen.")
                .isEqualTo(neu);
    }
}
