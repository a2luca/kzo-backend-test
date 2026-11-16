package de.firma.kuze.spectools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Vergleicht zwei Specs Operation für Operation.
 * <p>
 * Zwei Operationen mit gleicher {@code operationId} gelten als identisch, wenn HTTP-Methode, Pfad,
 * Parameter, Request-Body und Responses übereinstimmen. Dokumentationsschlüssel ({@link #IGNORIERT})
 * zählen nicht, ausgenommen Schema-Felder, die zufällig so heißen.
 */
public final class SpecVergleich {

    static final Set<String> IGNORIERT = Set.of("description", "summary", "example", "examples");

    private static final ObjectMapper MAPPER = Json.mapper();

    private SpecVergleich() {
    }

    public static VergleichsErgebnis vergleiche(Spec netze, Spec odr) {
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
