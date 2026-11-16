# Kundenzentrum-Backend: Beispielprojekt

Dieses Dokument ist der Auftrag für Claude Code. Lies es vollständig, bevor du anfängst, und arbeite dann die Schritte in Abschnitt 12 der Reihe nach ab.

## 1. Worum es geht

Wir planen ein neues Spring-Boot-REST-Backend für ein Kundenzentrum. Es spricht mit einem Angular-Frontend und ist direkt an SAP angebunden. Das Kundenzentrum gibt es für zwei Mandanten (Firmen): **Netze** und **ODR**. Jeder Mandant hat ein eigenes SAP-System und liefert eine eigene, komplette OpenAPI-Spec.

Manche SAP-Funktionen sind für beide Mandanten identisch, manche gibt es nur für einen, manche gibt es in beiden, aber mit unterschiedlicher Struktur. Trotzdem soll das Backend **eine Codebasis und ein Artefakt** sein. Pro Mandant läuft eine eigene Instanz desselben Artefakts, der Mandant wird beim Start per Profil festgelegt.

Dieses Beispielprojekt soll die Architektur im Kleinen zeigen und beweisen, dass sie funktioniert. Es ist kein Produktivcode. Wichtiger als Vollständigkeit ist, dass jemand aus dem Team das Projekt auschecken, bauen, starten und an vier kleinen Beispielen nachvollziehen kann, wie die vier Fälle umgesetzt sind.

## 2. Die vier Fälle, die das Beispiel zeigen muss

| Fall | Beispiel-Feature | Wo liegt die SAP-Anbindung |
|---|---|---|
| In beiden Specs identisch | `laender` | `adapter-common` |
| Nur in der Netze-Spec | `hausanschluss` | `adapter-netze` |
| Nur in der ODR-Spec | `tarif` | `adapter-odr` |
| In beiden, aber abweichend | `abschlag` | je eine Implementierung in `adapter-netze` und `adapter-odr` |

## 3. Architekturregeln

Diese Regeln sind der Kern des Projekts. Wenn eine davon technisch nicht so umsetzbar ist wie beschrieben, ändere nicht stillschweigend die Architektur, sondern dokumentiere die Abweichung im README und begründe sie.

1. **Der Core kennt kein SAP.** Im Modul `core` gibt es keine generierten Klassen, keine SAP-URLs und keinen HTTP-Code Richtung SAP. Der Core spricht nur über Port-Interfaces mit der Außenwelt.
2. **Generierte Klassen bleiben im Adapter.** Jeder Adapter mappt zwischen den Frontend-DTOs aus dem Core und den generierten SAP-Klassen. Es gibt kein drittes Modell dazwischen.
3. **Der Mandant wird beim Start festgelegt, nicht pro Request.** Im Code gibt es nirgends ein `if (mandant == ...)`. Unterschiede werden über Beans gelöst, die es je nach Mandant gibt oder nicht gibt.
4. **Der Frontend-Vertrag ist fest.** Pfade, Feldnamen und Statuscodes der REST-Schnittstelle stehen in Abschnitt 6 und dürfen nicht von der SAP-Struktur abhängen.
5. **Was gemeinsam ist, entscheidet der Spec-Vergleich.** Es gibt keine von Hand gepflegte Liste gemeinsamer Operationen. Ein Build-Schritt vergleicht beide Specs und teilt sie auf.
6. **Generierter Code wird nicht eingecheckt.** Er entsteht bei jedem Build unter `target/`.
7. **Abhängigkeiten nur in eine Richtung.** `core` hängt von keinem anderen Modul ab. `adapter-common` hängt von `core` ab. `adapter-netze` und `adapter-odr` hängen von `adapter-common` ab, aber nicht voneinander. `app` bindet alles zusammen.

## 4. Technik

- Java 21, Maven als Multi-Module-Projekt
- Spring Boot 3.x in der aktuellen stabilen Version, Spring MVC, kein WebFlux
- `openapi-generator-maven-plugin` in der aktuellen 7.x-Version, Generator `java`, Library `restclient`
- MapStruct für die Mapper
- JUnit 5, AssertJ, Spring Boot Test, WireMock, ArchUnit
- `swagger-parser` (io.swagger.parser.v3) für den Spec-Vergleich

Schlage die aktuellen Versionen nach, bevor du sie festlegst, und prüfe die Generator-Optionen gegen die Doku der gewählten Plugin-Version. Falls du Spring Boot 4 verwendest, braucht der Generator zusätzliche Optionen (`useSpringBoot4`, ggf. `useJackson3`). Bleib im Zweifel bei Spring Boot 3.x.

Basis-Package ist `de.firma.kuze` (Platzhalter).

## 5. Modulstruktur

```
kundenzentrum-backend/
├── pom.xml                      Parent: Versionen, Plugin-Management
├── README.md
├── specs/sap/
│   ├── netze.yaml               komplette Beispiel-Spec Netze
│   ├── odr.yaml                 komplette Beispiel-Spec ODR
│   └── vergleich.md             Bericht des Spec-Vergleichs, eingecheckt
│
├── spec-tools/                  Spec-Vergleich als kleines Java-Programm
├── core/                        Controller, Services, DTOs, Ports, Mandanten-Annotationen
├── adapter-common/              gemeinsame SAP-Verbindung + gemeinsame Funktionen
├── adapter-netze/               Funktionen nur für Netze + abweichende Funktionen
├── adapter-odr/                 Funktionen nur für ODR + abweichende Funktionen
├── app/                         Spring-Boot-Anwendung, Profile, Integrationstests
└── sap-mock/                    Attrappe für beide SAP-Systeme zum lokalen Starten
```

Im Core wird nach Features geschnitten, nicht nach Schichten:

```
core/src/main/java/de/firma/kuze/
├── mandant/          Mandant (Enum), @NurNetze, @NurOdr, Start-Validierung
├── fehler/           eigene Exceptions, @RestControllerAdvice
├── laender/          LaenderController, LandDto, LaenderService, LaenderPort
├── hausanschluss/    ... alles mit @NurNetze
├── tarif/            ... alles mit @NurOdr
└── abschlag/         AbschlagController, AbschlagDto, AbschlagService, AbschlagPort
```

Die Adapter spiegeln diese Feature-Ordner, z. B. `adapter-common/.../sap/common/laender/` mit `SapLaenderAdapter` und `LaenderMapper`.

## 6. Frontend-Vertrag (REST-Schnittstelle des Backends)

| Endpoint | Mandant | Antwort (Beispiel) |
|---|---|---|
| `GET /api/laender` | beide | `[{"code":"DE","name":"Deutschland"}]` |
| `GET /api/hausanschluesse/{id}` | nur Netze | `{"id":"4711","adresse":"Musterweg 1, 12345 Musterstadt","status":"IN_BETRIEB"}` |
| `GET /api/tarife` | nur ODR | `[{"id":"T1","name":"Strom Basis","grundpreisEuroProMonat":9.90}]` |
| `GET /api/abschlag/{vertragskontoId}` | beide | `{"betragEuro":85.00,"naechsteFaelligkeit":"2026-12-01"}` |

Fehler:

- Datensatz im SAP nicht gefunden: `404` mit `{"fehler":"NICHT_GEFUNDEN"}`
- SAP nicht erreichbar oder SAP-Fehler 5xx: `502` mit `{"fehler":"SAP_NICHT_VERFUEGBAR"}`
- Endpoint existiert für diesen Mandanten nicht: `404` wie bei jeder unbekannten URL

## 7. Beispiel-Specs für SAP

Lege `specs/sap/netze.yaml` und `specs/sap/odr.yaml` als OpenAPI 3.0 an. Die Feldnamen unterscheiden sich absichtlich vom Frontend-Vertrag, damit das Mapping sichtbar wird. Jede Operation braucht eine `operationId`.

| operationId | Pfad | netze.yaml | odr.yaml |
|---|---|---|---|
| `getLaender` | `GET /laender` | ja | ja, identisch |
| `getHausanschluss` | `GET /hausanschluesse/{anschlussNr}` | ja | nein |
| `getTarife` | `GET /tarife` | nein | ja |
| `getAbschlag` | `GET /abschlaege/{vertragskonto}` | ja | ja, anderes Response-Schema |

Response-Schemas:

- `getLaender`: Liste von `{ "landKz": "DE", "bezeichnung": "Deutschland" }`
- `getHausanschluss`: `{ "anschlussNr": "4711", "strasse": "Musterweg", "hausNr": "1", "plz": "12345", "ort": "Musterstadt", "statusKz": "B" }`. Statuskennzeichen: `B` = `IN_BETRIEB`, `P` = `IN_PLANUNG`, `S` = `STILLGELEGT`.
- `getTarife`: Liste von `{ "tarifId": "T1", "tarifBez": "Strom Basis", "grundpreisCent": 990 }`
- `getAbschlag` in Netze: `{ "betrag": 85.00, "faelligAm": "2026-12-01" }`
- `getAbschlag` in ODR: `{ "betragCent": 8500, "faelligkeit": "2026-12-01", "rhythmus": "MONATLICH" }`

Baue in `getLaender` absichtlich unterschiedliche Beschreibungstexte in die beiden Specs ein. Die Operation muss trotzdem als identisch erkannt werden.

## 8. Mandanten-Mechanik

- Property `kundenzentrum.mandant` mit den Werten `netze` oder `odr`.
- Profile `netze` und `odr` in `app/src/main/resources/application-netze.yaml` bzw. `application-odr.yaml`. Sie setzen den Mandanten und die SAP-Verbindung (`kundenzentrum.sap.base-url`, `user`, `password`).
- Zwei Annotationen im Core, jeweils als Meta-Annotation auf `@ConditionalOnProperty`:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(name = "kundenzentrum.mandant", havingValue = "netze")
public @interface NurNetze {}
```

- Bei einem Feature nur für einen Mandanten tragen Controller, Service, Adapter und die zugehörige Client-Konfiguration die Annotation. Das Port-Interface braucht keine.
- Bei einem abweichenden Feature (`abschlag`) sind Controller und Service im Core unmarkiert. Die beiden Adapter-Implementierungen tragen `@NurNetze` bzw. `@NurOdr`.
- Startet die Anwendung ohne gültigen Mandanten, bricht sie mit einer klaren Fehlermeldung ab.

Port-Belegung für lokale Starts: Backend Netze `8081`, Backend ODR `8082`, SAP-Mock Netze `9081`, SAP-Mock ODR `9082`.

## 9. Build-Pipeline für die generierten Clients

```
netze.yaml ─┐
            ├─► Spec-Vergleich ─► common.yaml ──────► Generator ─► client-common  (in adapter-common)
odr.yaml  ──┘                 ├─► netze-rest.yaml ──► Generator ─► client-netze   (in adapter-netze)
                              └─► odr-rest.yaml ────► Generator ─► client-odr     (in adapter-odr)
```

### Spec-Vergleich (`spec-tools`)

Ein kleines Java-Programm mit `main`-Methode. Eingabe sind die beiden Specs und ein Ausgabeordner.

- Beide Specs mit vollständig aufgelösten `$ref` einlesen.
- Operationen über `operationId` zuordnen.
- Zwei Operationen gelten als identisch, wenn HTTP-Methode, Pfad, Parameter, Request-Body und Responses übereinstimmen. Ignoriert werden `description`, `summary`, `example` und `examples`. Felder in Schemas, die zufällig `description` heißen, dürfen dabei nicht verloren gehen.
- Ausgabe sind drei Dateien: `common.yaml` (in beiden identisch), `netze-rest.yaml` und `odr-rest.yaml` (alles andere der jeweiligen Seite, also einseitige und abweichende Operationen).
- Die einfachste Lösung für die Ausgabedateien ist erlaubt: Pfade filtern und `components` vollständig übernehmen.
- Zusätzlich entsteht ein Bericht als Markdown mit drei Listen: gemeinsam, nur auf einer Seite, in beiden aber abweichend. Bei abweichenden Operationen steht dabei, was abweicht.

Erwartetes Ergebnis für die Beispiel-Specs:

- `common.yaml`: `getLaender`
- `netze-rest.yaml`: `getHausanschluss`, `getAbschlag`
- `odr-rest.yaml`: `getTarife`, `getAbschlag`

### Bericht einchecken und prüfen

Der Bericht liegt unter `specs/sap/vergleich.md` im Repository. Ein Test erzeugt ihn neu und vergleicht ihn mit der eingecheckten Fassung. Weicht er ab, schlägt der Build fehl. So wird im Commit sichtbar, wenn eine neue SAP-Spec eine Operation von gemeinsam nach abweichend verschiebt. Mit einem Schalter (z. B. `-Dspec.bericht.aktualisieren=true`) lässt sich die Datei bewusst neu schreiben.

### Generator

- Jedes Adapter-Modul führt in der Phase `generate-sources` zuerst den Spec-Vergleich aus (z. B. über `exec-maven-plugin`, Ausgabe nach `target/specs/`) und danach den Generator auf seiner Teil-Spec. Wenn du eine robustere Lösung für die Reihenfolge findest, nimm die und beschreibe sie im README.
- Packages: `de.firma.kuze.sap.common.client`, `de.firma.kuze.sap.netze.client`, `de.firma.kuze.sap.odr.client`, jeweils mit `.api` und `.model`.
- `modelNamePrefix` auf `Sap` setzen, damit generierte Klassen (`SapLand`) klar von den DTOs (`LandDto`) zu unterscheiden sind.
- `openApiNullable=false`, keine generierten Tests.

## 10. Adapter

### `adapter-common`

- `SapProperties` (`@ConfigurationProperties("kundenzentrum.sap")`) mit `baseUrl`, `user`, `password`.
- Ein gemeinsamer `RestClient` als Bean mit Authentifizierung (im Beispiel Basic Auth). Den benutzen auch `adapter-netze` und `adapter-odr`.
- Der generierte `ApiClient` aus `client-common` als Bean, mit der Base-URL aus den Properties.
- `SapLaenderAdapter implements LaenderPort` plus `LaenderMapper`.
- Übersetzung von SAP-Fehlern in die Exceptions aus dem Core: SAP `404` wird `NichtGefundenException`, SAP `5xx` und Verbindungsfehler werden `SapNichtVerfuegbarException`. Löse das an einer zentralen Stelle und belege mit einem Test, dass es mit dem generierten Client tatsächlich greift.

In diesem Modul gibt es keine Mandanten-Annotationen. Derselbe Code läuft in beiden Instanzen, nur die Base-URL unterscheidet sich.

### `adapter-netze` und `adapter-odr`

- Eigene Konfigurationsklasse mit `@NurNetze` bzw. `@NurOdr`, die den generierten `ApiClient` des jeweiligen Clients und die API-Klassen als Beans anmeldet.
- `adapter-netze`: `SapHausanschlussAdapter` (setzt die Adresse aus den Einzelfeldern zusammen und übersetzt das Statuskennzeichen), `SapAbschlagAdapter`.
- `adapter-odr`: `SapTarifAdapter` (rechnet Cent in Euro um), `SapAbschlagAdapter` (rechnet Cent in Euro um).
- Jeweils MapStruct-Mapper mit `componentModel = "spring"`.

## 11. SAP-Mock

Ein kleines Modul, das die SAP-Endpunkte aus den Specs mit festen Beispielantworten bedient, damit man beide Backend-Instanzen lokal starten und ausprobieren kann. Es lässt sich je Mandant starten (Netze auf `9081`, ODR auf `9082`) und liefert die Beispielwerte aus Abschnitt 7. Ob du dafür WireMock einbettest oder eine kleine Spring-Boot-Anwendung schreibst, ist dir überlassen. Docker soll nicht nötig sein.

## 12. Vorgehen

Arbeite in dieser Reihenfolge und stelle nach jedem Schritt sicher, dass `mvn verify` grün ist.

1. Parent-POM und leere Module anlegen.
2. Beispiel-Specs schreiben.
3. `spec-tools` mit Spec-Vergleich, Bericht und Unit-Tests.
4. `core` mit Mandanten-Paket, Fehlerbehandlung und dem Feature `laender`.
5. `adapter-common` mit Generator, SAP-Verbindung und `SapLaenderAdapter`.
6. `app` mit Profilen. Erster Durchstich: `GET /api/laender` in beiden Profilen gegen WireMock.
7. Feature `hausanschluss` (nur Netze) durch Core und `adapter-netze`.
8. Feature `tarif` (nur ODR) durch Core und `adapter-odr`.
9. Feature `abschlag` (abweichend) mit zwei Adapter-Implementierungen.
10. `sap-mock` und README.
11. ArchUnit-Regeln und Abschlussprüfung gegen Abschnitt 13.

## 13. Abnahmekriterien

Das Projekt ist fertig, wenn alle Punkte erfüllt sind:

- `mvn verify` läuft im Wurzelverzeichnis ohne Fehler durch.
- Unter `src/` liegt kein generierter Code.
- Der Spec-Vergleich liefert für die Beispiel-Specs genau die Aufteilung aus Abschnitt 9. Dafür gibt es Unit-Tests, auch für den Fall „nur Beschreibungstext unterschiedlich“.
- Der eingecheckte Bericht `specs/sap/vergleich.md` stimmt, und der Test dazu schlägt fehl, wenn man eine Spec ändert.
- Es gibt je einen Spring-Context-Test für die Profile `netze` und `odr`.
- Im Profil `odr` liefert `GET /api/hausanschluesse/4711` den Status `404`, im Profil `netze` liefert `GET /api/tarife` den Status `404`. Beides ist als Test abgesichert.
- `GET /api/abschlag/{id}` liefert in beiden Profilen dieselbe Antwortstruktur, obwohl die SAP-Antworten unterschiedlich sind.
- Für jeden Adapter gibt es einen Test, der eine SAP-Beispielantwort hineingibt und das erwartete Frontend-DTO prüft.
- Die Fehlerübersetzung (SAP `404` und SAP `500`) ist für mindestens einen Endpoint von außen getestet.
- ArchUnit prüft: `core` greift nicht auf Packages unter `de.firma.kuze.sap` zu, und `adapter-netze` und `adapter-odr` greifen nicht aufeinander zu.
- Start ohne Profil bricht mit einer verständlichen Meldung ab.
- Nach Anleitung im README lassen sich beide Mocks und beide Backend-Instanzen starten, und die vier Endpoints antworten wie in Abschnitt 6.

## 14. README

Schreibe ein `README.md` für Kolleginnen und Kollegen, die das Projekt zum ersten Mal sehen. Inhalt:

- die Idee in wenigen Sätzen und die vier Fälle mit Verweis auf die Beispiel-Features
- Bauen und Starten beider Instanzen samt Mocks, mit `curl`-Beispielen
- „So füge ich eine neue Funktion hinzu“ für alle vier Fälle als kurze Schrittfolge
- was zu tun ist, wenn SAP eine neue Spec liefert
- Abweichungen von diesem Dokument und offene Punkte

## 15. Nicht im Umfang

- Anmeldung und Berechtigungen (keine Spring Security im Beispiel)
- ein echtes SAP oder echte SAP-Specs
- Frontend, CI-Pipeline, Docker, Deployment
- Datenbank oder Caching

## 16. Stellen, an denen du genau hinschauen sollst

Diese Punkte sind im Konzept noch nicht praktisch erprobt. Prüfe sie und halte das Ergebnis im README fest.

- Reihenfolge von Spec-Vergleich und Generator innerhalb von `generate-sources`, auch wenn nur ein einzelnes Modul gebaut wird.
- Konstruktor und Methoden des generierten `ApiClient` in der gewählten Generator-Version.
- Ob die Fehlerbehandlung am gemeinsamen `RestClient` beim generierten Client greift oder ob sie an anderer Stelle sitzen muss.
- Ob bei gefilterten Teil-Specs unbenutzte Modellklassen entstehen und ob das stört.
- Zwei `SapAbschlagAdapter` mit gleichem Klassennamen in verschiedenen Modulen: Prüfe, dass es keine Kollision bei Bean-Namen gibt, und benenne sie um, falls doch.
