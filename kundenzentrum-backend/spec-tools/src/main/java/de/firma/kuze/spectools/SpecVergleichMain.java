package de.firma.kuze.spectools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Aufruf: {@code SpecVergleichMain <netze.yaml> <odr.yaml> <ausgabeordner>}
 * <p>
 * Schreibt {@code common.yaml}, {@code netze-rest.yaml}, {@code odr-rest.yaml} und {@code vergleich.md}
 * in den Ausgabeordner.
 */
public final class SpecVergleichMain {

    private SpecVergleichMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Aufruf: SpecVergleichMain <netze.yaml> <odr.yaml> <ausgabeordner>");
            System.exit(2);
        }
        Spec netze = Spec.laden("netze.yaml", Path.of(args[0]));
        Spec odr = Spec.laden("odr.yaml", Path.of(args[1]));
        Path ausgabe = Path.of(args[2]);
        Files.createDirectories(ausgabe);

        VergleichsErgebnis ergebnis = SpecVergleich.vergleiche(netze, odr);
        schreiben(ausgabe.resolve("common.yaml"),
                TeilSpec.alsYaml(TeilSpec.bauen(netze, ergebnis.idsGemeinsam(), "gemeinsamer Teil")));
        schreiben(ausgabe.resolve("netze-rest.yaml"),
                TeilSpec.alsYaml(TeilSpec.bauen(netze, ergebnis.idsNetzeRest(), "nur Netze und abweichend")));
        schreiben(ausgabe.resolve("odr-rest.yaml"),
                TeilSpec.alsYaml(TeilSpec.bauen(odr, ergebnis.idsOdrRest(), "nur ODR und abweichend")));
        schreiben(ausgabe.resolve("vergleich.md"), Bericht.schreiben(ergebnis));

        System.out.printf("Spec-Vergleich: gemeinsam %s, Netze-Rest %s, ODR-Rest %s%n",
                ergebnis.idsGemeinsam(), ergebnis.idsNetzeRest(), ergebnis.idsOdrRest());
    }

    private static void schreiben(Path datei, String inhalt) throws IOException {
        Files.writeString(datei, inhalt);
    }
}
