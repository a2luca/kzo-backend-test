package de.firma.kuze.spectools;

import java.util.List;
import java.util.stream.Stream;

/** Markdown-Bericht über die Aufteilung. Enthält bewusst keinen Zeitstempel, damit er stabil vergleichbar bleibt. */
public final class Bericht {

    private Bericht() {
    }

    public static String schreiben(VergleichsErgebnis ergebnis) {
        var md = new StringBuilder();
        md.append("# Spec-Vergleich Netze / ODR\n\n");
        md.append("Erzeugt von `spec-tools` aus `netze.yaml` und `odr.yaml`. Nicht von Hand bearbeiten,\n");
        md.append("sondern mit `./gradlew :spec-tools:test -Dspec.bericht.aktualisieren=true` neu schreiben.\n\n");

        md.append("## Gemeinsam (→ `common.yaml`)\n\n");
        liste(md, ergebnis.gemeinsam().stream().map(Bericht::zeile).toList());

        md.append("## Nur auf einer Seite\n\n");
        liste(md, Stream.concat(
                ergebnis.nurNetze().stream().map(op -> zeile(op) + " – nur Netze (→ `netze-rest.yaml`)"),
                ergebnis.nurOdr().stream().map(op -> zeile(op) + " – nur ODR (→ `odr-rest.yaml`)"))
                .sorted().toList());

        md.append("## In beiden, aber abweichend (→ `netze-rest.yaml` und `odr-rest.yaml`)\n\n");
        if (ergebnis.abweichend().isEmpty()) {
            md.append("_keine_\n\n");
        }
        for (var abweichung : ergebnis.abweichend()) {
            md.append("### `").append(abweichung.operationId()).append("`\n\n");
            md.append("Netze: ").append(abweichung.netze().anzeige())
                    .append(", ODR: ").append(abweichung.odr().anzeige()).append("\n\n");
            abweichung.unterschiede().forEach(u -> md.append("- ").append(u).append('\n'));
            md.append('\n');
        }
        return md.toString().stripTrailing() + "\n";
    }

    private static String zeile(SpecOperation op) {
        return "`" + op.operationId() + "` – " + op.anzeige();
    }

    private static void liste(StringBuilder md, List<String> zeilen) {
        if (zeilen.isEmpty()) {
            md.append("_keine_\n\n");
            return;
        }
        zeilen.forEach(z -> md.append("- ").append(z).append('\n'));
        md.append('\n');
    }
}
