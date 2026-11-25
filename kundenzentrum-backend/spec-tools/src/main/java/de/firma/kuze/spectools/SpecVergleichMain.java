package de.firma.kuze.spectools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Aufruf: {@code SpecVergleichMain <netze.yaml> <odr.yaml> <ausgabeordner>}
 * <p>
 * Schreibt {@code common.yaml}, {@code netze-rest.yaml}, {@code odr-rest.yaml} und {@code vergleich.md}
 * in den Ausgabeordner. Die restlichen Klassen dieser Datei (Spec, SpecOperation, SpecVergleich,
 * VergleichsErgebnis, TeilSpec, Bericht) sind nur innerhalb von {@code spectools} sichtbar, weil nichts
 * außerhalb des Pakets sie braucht.
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

/**
 * Eine SAP-Spec. Für den Vergleich wird sie mit vollständig aufgelösten {@code $ref} gelesen,
 * für die Teil-Specs als unveränderte Kopie, damit die {@code $ref} auf {@code components} erhalten bleiben.
 */
record Spec(String name, String inhalt) {

    static Spec laden(String name, Path datei) {
        try {
            return new Spec(name, Files.readString(datei));
        } catch (IOException e) {
            throw new UncheckedIOException("Spec " + datei + " kann nicht gelesen werden", e);
        }
    }

    OpenAPI aufgeloest() {
        var optionen = new ParseOptions();
        optionen.setResolve(true);
        optionen.setResolveFully(true);
        return parsen(optionen);
    }

    /** Liefert bei jedem Aufruf eine neue Kopie, die verändert werden darf. */
    OpenAPI roh() {
        var optionen = new ParseOptions();
        optionen.setResolve(false);
        return parsen(optionen);
    }

    private OpenAPI parsen(ParseOptions optionen) {
        SwaggerParseResult ergebnis = new OpenAPIParser().readContents(inhalt, null, optionen);
        if (ergebnis.getOpenAPI() == null) {
            throw new IllegalArgumentException("Spec " + name + " ist ungültig: " + ergebnis.getMessages());
        }
        return ergebnis.getOpenAPI();
    }
}

/** Eine Operation mit dem Ort, an dem sie in der Spec steht. */
record SpecOperation(String operationId, PathItem.HttpMethod methode, String pfad,
                      PathItem pathItem, Operation operation) {

    String anzeige() {
        return methode + " " + pfad;
    }

    /** Alle Operationen einer Spec, sortiert nach {@code operationId}. */
    static Map<String, SpecOperation> alle(OpenAPI api, String specName) {
        var ergebnis = new TreeMap<String, SpecOperation>();
        if (api.getPaths() == null) {
            return ergebnis;
        }
        api.getPaths().forEach((pfad, pathItem) -> pathItem.readOperationsMap().forEach((methode, operation) -> {
            String id = operation.getOperationId();
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Spec " + specName + ": Operation " + methode + " " + pfad + " hat keine operationId");
            }
            var vorher = ergebnis.put(id, new SpecOperation(id, methode, pfad, pathItem, operation));
            if (vorher != null) {
                throw new IllegalArgumentException(
                        "Spec " + specName + ": operationId " + id + " ist doppelt vergeben");
            }
        }));
        return ergebnis;
    }
}

/**
 * Vergleicht zwei Specs Operation für Operation.
 * <p>
 * Zwei Operationen mit gleicher {@code operationId} gelten als identisch, wenn HTTP-Methode, Pfad,
 * Parameter, Request-Body und Responses übereinstimmen. Dokumentationsschlüssel ({@link #IGNORIERT})
 * zählen nicht, ausgenommen Schema-Felder, die zufällig so heißen.
 */
final class SpecVergleich {

    static final Set<String> IGNORIERT = Set.of("description", "summary", "example", "examples");

    private static final ObjectMapper MAPPER = Json.mapper();

    private SpecVergleich() {
    }

    static VergleichsErgebnis vergleiche(Spec netze, Spec odr) {
        Map<String, SpecOperation> opsNetze = SpecOperation.alle(netze.aufgeloest(), netze.name());
        Map<String, SpecOperation> opsOdr = SpecOperation.alle(odr.aufgeloest(), odr.name());

        var gemeinsam = new ArrayList<SpecOperation>();
        var nurNetze = new ArrayList<SpecOperation>();
        var nurOdr = new ArrayList<SpecOperation>();
        var abweichend = new ArrayList<VergleichsErgebnis.Abweichung>();

        var alleIds = new TreeSet<String>();
        alleIds.addAll(opsNetze.keySet());
        alleIds.addAll(opsOdr.keySet());

        for (String id : alleIds) {
            SpecOperation n = opsNetze.get(id);
            SpecOperation o = opsOdr.get(id);
            if (o == null) {
                nurNetze.add(n);
            } else if (n == null) {
                nurOdr.add(o);
            } else {
                var unterschiede = new ArrayList<String>();
                unterschiede(List.of(), vergleichsForm(n), vergleichsForm(o), unterschiede);
                if (unterschiede.isEmpty()) {
                    gemeinsam.add(n);
                } else {
                    abweichend.add(new VergleichsErgebnis.Abweichung(n, o, unterschiede));
                }
            }
        }
        return new VergleichsErgebnis(gemeinsam, nurNetze, nurOdr, abweichend);
    }

    /** Die Teile einer Operation, die über "identisch" entscheiden, ohne Dokumentationsschlüssel. */
    static JsonNode vergleichsForm(SpecOperation op) {
        var parameter = new ArrayList<Parameter>();
        if (op.pathItem().getParameters() != null) {
            parameter.addAll(op.pathItem().getParameters());
        }
        if (op.operation().getParameters() != null) {
            parameter.addAll(op.operation().getParameters());
        }
        ObjectNode form = MAPPER.createObjectNode();
        form.put("methode", op.methode().name());
        form.put("pfad", op.pfad());
        form.set("parameters", MAPPER.valueToTree(parameter));
        form.set("requestBody", MAPPER.valueToTree(op.operation().getRequestBody()));
        form.set("responses", MAPPER.valueToTree(op.operation().getResponses()));
        return bereinigen(form, false);
    }

    /**
     * Entfernt Dokumentationsschlüssel rekursiv. Unter {@code properties} sind die Schlüssel
     * Feldnamen des Schemas, die bleiben auch dann erhalten, wenn sie {@code description} heißen.
     */
    static JsonNode bereinigen(JsonNode knoten, boolean schluesselSindFeldnamen) {
        if (knoten.isObject()) {
            ObjectNode sauber = MAPPER.createObjectNode();
            knoten.properties().forEach(eintrag -> {
                String schluessel = eintrag.getKey();
                if (!schluesselSindFeldnamen && IGNORIERT.contains(schluessel)) {
                    return;
                }
                boolean kinderSindFeldnamen = !schluesselSindFeldnamen && schluessel.equals("properties");
                sauber.set(schluessel, bereinigen(eintrag.getValue(), kinderSindFeldnamen));
            });
            return sauber;
        }
        if (knoten.isArray()) {
            ArrayNode sauber = MAPPER.createArrayNode();
            knoten.forEach(element -> sauber.add(bereinigen(element, false)));
            return sauber;
        }
        return knoten;
    }

    static void unterschiede(List<String> pfad, JsonNode netze, JsonNode odr, List<String> ergebnis) {
        if (netze.equals(odr)) {
            return;
        }
        String ort = pfad.isEmpty() ? "Operation" : String.join(" › ", pfad);
        if (netze.isObject() && odr.isObject()) {
            var nurNetze = new TreeSet<String>();
            var nurOdr = new TreeSet<String>();
            var beide = new TreeSet<String>();
            netze.fieldNames().forEachRemaining(n -> (odr.has(n) ? beide : nurNetze).add(n));
            odr.fieldNames().forEachRemaining(n -> {
                if (!netze.has(n)) {
                    nurOdr.add(n);
                }
            });
            if (!nurNetze.isEmpty() || !nurOdr.isEmpty()) {
                var text = new StringBuilder("`" + ort + "`:");
                if (!nurNetze.isEmpty()) {
                    text.append(" nur in Netze ").append(liste(nurNetze));
                }
                if (!nurNetze.isEmpty() && !nurOdr.isEmpty()) {
                    text.append(";");
                }
                if (!nurOdr.isEmpty()) {
                    text.append(" nur in ODR ").append(liste(nurOdr));
                }
                ergebnis.add(text.toString());
            }
            for (String name : beide) {
                var unterpfad = new ArrayList<>(pfad);
                unterpfad.add(name);
                unterschiede(unterpfad, netze.get(name), odr.get(name), ergebnis);
            }
            return;
        }
        if (netze.isArray() && odr.isArray() && netze.size() == odr.size()) {
            for (int i = 0; i < netze.size(); i++) {
                var unterpfad = new ArrayList<>(pfad);
                unterpfad.add("[" + i + "]");
                unterschiede(unterpfad, netze.get(i), odr.get(i), ergebnis);
            }
            return;
        }
        ergebnis.add("`" + ort + "`: Netze `" + netze + "`, ODR `" + odr + "`");
    }

    private static String liste(Set<String> namen) {
        return String.join(", ", namen.stream().map(n -> "`" + n + "`").toList());
    }
}

record VergleichsErgebnis(List<SpecOperation> gemeinsam,
                           List<SpecOperation> nurNetze,
                           List<SpecOperation> nurOdr,
                           List<Abweichung> abweichend) {

    record Abweichung(SpecOperation netze, SpecOperation odr, List<String> unterschiede) {
        String operationId() {
            return netze.operationId();
        }
    }

    /** Inhalt von {@code common.yaml}. */
    Set<String> idsGemeinsam() {
        return ids(gemeinsam.stream().map(SpecOperation::operationId));
    }

    /** Inhalt von {@code netze-rest.yaml}: nur in Netze plus abweichend. */
    Set<String> idsNetzeRest() {
        return ids(Stream.concat(nurNetze.stream().map(SpecOperation::operationId),
                abweichend.stream().map(Abweichung::operationId)));
    }

    /** Inhalt von {@code odr-rest.yaml}: nur in ODR plus abweichend. */
    Set<String> idsOdrRest() {
        return ids(Stream.concat(nurOdr.stream().map(SpecOperation::operationId),
                abweichend.stream().map(Abweichung::operationId)));
    }

    private static Set<String> ids(Stream<String> ids) {
        return ids.collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
    }
}

/**
 * Baut eine Teil-Spec: nur die gewünschten Operationen, {@code components} unverändert.
 * Das ist die einfachste erlaubte Lösung. Folge: Der Generator erzeugt auch Modellklassen,
 * die in der Teil-Spec von keiner Operation benutzt werden.
 */
final class TeilSpec {

    private TeilSpec() {
    }

    static OpenAPI bauen(Spec quelle, Set<String> operationIds, String titelZusatz) {
        OpenAPI api = quelle.roh();
        var gefiltert = new Paths();
        if (api.getPaths() != null) {
            api.getPaths().forEach((pfad, pathItem) -> {
                for (PathItem.HttpMethod methode : PathItem.HttpMethod.values()) {
                    Operation op = pathItem.readOperationsMap().get(methode);
                    if (op != null && !operationIds.contains(op.getOperationId())) {
                        pathItem.operation(methode, null);
                    }
                }
                if (!pathItem.readOperations().isEmpty()) {
                    gefiltert.addPathItem(pfad, pathItem);
                }
            });
        }
        api.setPaths(gefiltert);
        api.getInfo().setTitle(api.getInfo().getTitle() + " – " + titelZusatz);
        return api;
    }

    static String alsYaml(OpenAPI api) {
        try {
            return Yaml.pretty().writeValueAsString(api);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}

/** Markdown-Bericht über die Aufteilung. Enthält bewusst keinen Zeitstempel, damit er stabil vergleichbar bleibt. */
final class Bericht {

    private Bericht() {
    }

    static String schreiben(VergleichsErgebnis ergebnis) {
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
