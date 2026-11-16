# SAP-Clients aus den Specs generieren

Diese Anleitung erklärt, wie aus `specs/sap/netze.yaml` und `specs/sap/odr.yaml` die Java-Klassen für die
drei SAP-Clients (Common, Netze, ODR) entstehen, wie man sie erzeugt, wo man sie findet und was man
dabei beachten muss.

**Kurz:** Man generiert nie von Hand. `./gradlew build` (oder schon `compileJava`) erledigt alles. Die
generierten Klassen liegen unter `<modul>/build/generated/sap-client/` und werden nicht eingecheckt.

## 1. Was passiert beim Generieren

```
specs/sap/netze.yaml ─┐
                      ├─► :spec-tools:specsAufteilen ─► spec-tools/build/specs/common.yaml ─────► :adapter-common:sapClientGenerieren
specs/sap/odr.yaml  ──┘                               ├► spec-tools/build/specs/netze-rest.yaml ──► :adapter-netze:sapClientGenerieren
                                                      └► spec-tools/build/specs/odr-rest.yaml ────► :adapter-odr:sapClientGenerieren
```

1. **Spec-Vergleich** (`:spec-tools:specsAufteilen`): liest beide Specs mit aufgelösten `$ref` und ordnet
   die Operationen über ihre `operationId` zu. Daraus entstehen drei Teil-Specs:

   | Datei | Inhalt | Client | Modul |
   |---|---|---|---|
   | `common.yaml` | Operationen, die in beiden Specs **identisch** sind | `de.firma.kuze.sap.common.client` | `adapter-common` |
   | `netze-rest.yaml` | nur in Netze **plus** in beiden, aber abweichend | `de.firma.kuze.sap.netze.client` | `adapter-netze` |
   | `odr-rest.yaml` | nur in ODR **plus** in beiden, aber abweichend | `de.firma.kuze.sap.odr.client` | `adapter-odr` |

   „Identisch“ heißt: gleiche HTTP-Methode, gleicher Pfad, gleiche Parameter, gleicher Request-Body und
   gleiche Responses. `description`, `summary`, `example` und `examples` zählen nicht.

2. **Generator** (`:<adapter>:sapClientGenerieren`): Der OpenAPI-Generator (`java`, Library `restclient`)
   erzeugt aus der jeweiligen Teil-Spec den Client nach `<modul>/build/generated/sap-client/src/main/java`.
   Gradle hängt dieses Verzeichnis automatisch als Quellordner ein.

Die Reihenfolge stellt Gradle selbst sicher: Der Generator bekommt die Teil-Specs als Eingabe aus
`spec-tools`, also läuft der Vergleich immer vorher. Das gilt auch, wenn man nur ein einzelnes Modul baut.
Beide Schritte sind gecacht und laufen nur erneut, wenn sich eine Spec geändert hat.

## 2. Generieren auslösen

| Ziel | Kommandozeile | VS Code (Gradle-Seitenleiste) |
|---|---|---|
| Alles generieren und kompilieren | `./gradlew compileJava` | im VS-Code-Terminal per Kommandozeile, oder `app` → `build` → `classes` (baut alle Module, von denen `app` abhängt) |
| Alles inkl. Tests (vor jedem Commit) | `./gradlew build` | im VS-Code-Terminal per Kommandozeile |
| Nur die Specs aufteilen | `./gradlew :spec-tools:specsAufteilen` | `spec-tools` → `build` → `specsAufteilen` |
| Nur den Common-Client | `./gradlew :adapter-common:sapClientGenerieren` | `adapter-common` → `build` → `sapClientGenerieren` |
| Nur den Netze-Client | `./gradlew :adapter-netze:sapClientGenerieren` | `adapter-netze` → `build` → `sapClientGenerieren` |
| Nur den ODR-Client | `./gradlew :adapter-odr:sapClientGenerieren` | `adapter-odr` → `build` → `sapClientGenerieren` |

Nach dem Generieren in VS Code **„Java: Reload Projects“** ausführen (Befehlspalette, Cmd+Shift+P),
falls neue Klassen im Editor noch rot unterstrichen sind.

## 3. Wo die Ergebnisse liegen

```
spec-tools/build/specs/
├── common.yaml, netze-rest.yaml, odr-rest.yaml    Teil-Specs (zum Nachschauen, was wohin ging)
└── vergleich.md                                   Bericht, identisch mit specs/sap/vergleich.md

adapter-common/build/generated/sap-client/src/main/java/de/firma/kuze/sap/common/client/
├── ApiClient.java                                 HTTP-Grundgerüst des Clients
├── api/LaenderApi.java                            eine Klasse pro Tag
└── model/SapLand.java, ...                        eine Klasse pro Schema, Präfix "Sap"

adapter-netze/build/generated/sap-client/src/main/java/de/firma/kuze/sap/netze/client/...
adapter-odr/build/generated/sap-client/src/main/java/de/firma/kuze/sap/odr/client/...
```

Auf einen Blick sieht man die Aufteilung in `specs/sap/vergleich.md`.

## 4. Wie Namen in der Spec zu Klassen werden

| In der Spec | Wird zu | Beispiel |
|---|---|---|
| `tags: [Laender]` an der Operation | API-Klasse `<Tag>Api` | `LaenderApi` (ohne Tag: `DefaultApi`) |
| `operationId: getLaender` | Methode in der API-Klasse | `List<SapLand> getLaender()` |
| Pfad-/Query-Parameter | Methodenparameter | `getHausanschluss(String anschlussNr)` |
| `components/schemas/Land` | Modellklasse mit Präfix `Sap` | `SapLand` mit `getLandKz()`, `getBezeichnung()` |
| `enum: [B, P, S]` an einer Property | inneres Enum | `SapHausanschluss.StatusKzEnum.B` |
| `type: string, format: date` | `LocalDate` | |
| `type: number, format: double` | `Double` | |
| `type: integer, format: int64` | `Long` | |

Das Präfix `Sap` sorgt dafür, dass generierte Klassen (`SapLand`) nicht mit den DTOs im Core
(`LandDto`) verwechselt werden.

## 5. Regeln für die Specs

- **Jede Operation braucht eine `operationId`.** Fehlt sie, bricht der Spec-Vergleich mit einer Meldung
  wie `Operation GET /x hat keine operationId` ab.
- **Dieselbe Funktion muss in beiden Specs dieselbe `operationId` haben**, sonst gilt sie als
  „nur Netze“ und „nur ODR“ statt als gemeinsam.
- **Tags gemeinsamer Operationen gleich halten.** Tags werden nicht verglichen, bestimmen aber den
  Klassennamen. `common.yaml` wird aus der Netze-Spec gebaut, im Zweifel gilt also deren Tag.
- **Beschreibungstexte dürfen abweichen.** Felder in Schemas, die zufällig `description` heißen, werden
  trotzdem verglichen.
- **Reihenfolgen zählen.** Unterscheidet sich nur die Reihenfolge von Parametern oder Enum-Werten,
  gilt die Operation als abweichend.

## 6. Eine Spec ändern

1. `specs/sap/netze.yaml` und/oder `odr.yaml` bearbeiten oder durch eine neue SAP-Lieferung ersetzen.
2. `./gradlew build` ausführen.
   - Ändert sich die Aufteilung, schlägt `BerichtTest` fehl: *„specs/sap/vergleich.md passt nicht mehr zu den Specs“*.
   - Ist eine Operation aus einem Client verschwunden oder hat sich ihre Struktur geändert, zeigt der
     Compiler die Adapter, die angepasst werden müssen.
3. Bericht bewusst neu schreiben:
   ```bash
   ./gradlew :spec-tools:test -Dspec.bericht.aktualisieren=true
   ```
4. Im Diff von `specs/sap/vergleich.md` prüfen, was sich verschoben hat. Bei abweichenden Operationen
   steht dort, an welcher Stelle sie sich unterscheiden, z. B.
   `responses › 200 › … › properties: nur in Netze betrag; nur in ODR betragCent`.
5. Wieder `./gradlew build`, dann Spec und Bericht zusammen committen.

**Typischer Fall:** Eine bisher gemeinsame Operation weicht in einer neuen Spec ab. Dann verschwindet ihre
API-Klasse aus `adapter-common`, und `SapXyzAdapter` in `adapter-common` kompiliert nicht mehr. Das Feature
wird damit von Fall 1 zu Fall 4 (siehe [neues-feature.md](neues-feature.md)): Der Adapter zieht nach
`adapter-netze` und `adapter-odr` um.

## 7. Was man nicht tun darf

- **Generierten Code nicht bearbeiten und nicht nach `src/` kopieren.** Er wird bei jedem Build
  überschrieben. `KeinGenerierterCodeTest` schlägt fehl, wenn generierter Code unter `src/` liegt.
- **Generierte Klassen nicht im Core verwenden.** Sie bleiben im jeweiligen Adapter, die Mapper übersetzen
  in die Core-DTOs. `ArchitekturTest` prüft das.
- **`netze`-Client-Klassen nicht in `adapter-odr` benutzen und umgekehrt.** Auch das prüft ArchUnit.
- **Keine Liste gemeinsamer Operationen von Hand pflegen.** Was gemeinsam ist, entscheidet allein der
  Spec-Vergleich.

## 8. Generator-Einstellungen

Die Einstellungen stehen an einer Stelle: `buildSrc/src/main/kotlin/kuze.sap-client.gradle.kts`. Jedes
Adapter-Modul gibt nur noch Teil-Spec und Package an, z. B. in `adapter-netze/build.gradle.kts`:

```kotlin
plugins {
    id("kuze.sap-client")
}

sapClient {
    teilSpec = "netze-rest.yaml"
    paket = "de.firma.kuze.sap.netze.client"
}
```

Wichtige Optionen: Generator `java`, Library `restclient`, `modelNamePrefix = "Sap"`,
`openApiNullable=false`, `useJakartaEe=true`, `dateLibrary=java8`, keine generierten Tests und keine
Dokumentation. Die Version des Generators steht in `gradle/libs.versions.toml` (`openapi-generator`).

## 9. Häufige Probleme

| Symptom | Ursache | Abhilfe |
|---|---|---|
| VS Code: `SapXyz cannot be resolved`, Build auf der Kommandozeile läuft aber | VS Code kennt die generierten Quellen noch nicht | `./gradlew compileJava`, dann „Java: Reload Projects“ |
| `BerichtTest` schlägt fehl | Spec-Änderung verschiebt Operationen | Abschnitt 6, Schritt 3–5 |
| `Operation … hat keine operationId` | Operation ohne `operationId` in einer Spec | `operationId` ergänzen |
| Operation landet unerwartet in „abweichend“ | echter Strukturunterschied, auch Reihenfolge oder Typ | Unterschiede in `vergleich.md` ansehen, mit SAP klären |
| Klasse heißt `DefaultApi` | Operation hat kein `tags` | Tag in der Spec ergänzen |
| Modelle wie `SapHausanschluss` im Common-Client | Teil-Specs übernehmen alle `components` | gewollt und harmlos, siehe README „Offene Punkte“ |
