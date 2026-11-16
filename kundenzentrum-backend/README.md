# Kundenzentrum-Backend (Beispielprojekt)

Ein Spring-Boot-REST-Backend für das Kundenzentrum der Mandanten **Netze** und **ODR**. Jeder Mandant hat
ein eigenes SAP-System mit eigener OpenAPI-Spec. Trotzdem gibt es **eine Codebasis und ein Artefakt**.
Pro Mandant läuft eine eigene Instanz, das Profil (`netze` oder `odr`) legt beim Start fest, welche Beans
es gibt. Im Code steht nirgends `if (mandant == ...)`.

Welche SAP-Operationen beiden Mandanten gemeinsam sind, pflegt niemand von Hand. Das entscheidet
bei jedem Build ein Vergleich der beiden Specs (`spec-tools`).

Das Projekt ist kein Produktivcode. Es zeigt die Architektur an vier kleinen Beispielen:

| Fall | Feature | Frontend-Endpoint | SAP-Anbindung | Mandanten-Annotation |
|---|---|---|---|---|
| In beiden Specs identisch | `laender` | `GET /api/laender` | `adapter-common` | keine |
| Nur in der Netze-Spec | `hausanschluss` | `GET /api/hausanschluesse/{id}` | `adapter-netze` | `@NurNetze` auf Controller, Service, Adapter, Client-Konfiguration |
| Nur in der ODR-Spec | `tarif` | `GET /api/tarife` | `adapter-odr` | `@NurOdr` auf Controller, Service, Adapter, Client-Konfiguration |
| In beiden, aber abweichend | `abschlag` | `GET /api/abschlag/{vertragskontoId}` | je ein `SapAbschlagAdapter` in `adapter-netze` und `adapter-odr` | Controller/Service unmarkiert, Adapter `@NurNetze` bzw. `@NurOdr` |

## Weitere Anleitungen

- [docs/sap-clients-generieren.md](docs/sap-clients-generieren.md): Wie aus den SAP-Specs die Klassen für Common, Netze und ODR entstehen
- [docs/neues-feature.md](docs/neues-feature.md): Ein neues Feature umsetzen, Schritt für Schritt für alle vier Fälle

## Aufbau

```
specs/sap/netze.yaml, odr.yaml   Beispiel-Specs der beiden SAP-Systeme
specs/sap/vergleich.md           Bericht des Spec-Vergleichs (eingecheckt, per Test geprüft)
buildSrc/                        Gradle-Konventionen: kuze.java-conventions, kuze.sap-client
spec-tools/                      Spec-Vergleich (Java, main-Methode) + Task specsAufteilen
core/                            Controller, Services, DTOs, Ports, Mandanten-Mechanik, Fehlerbehandlung. Kein SAP.
adapter-common/                  gemeinsamer RestClient, Fehlerübersetzung, client-common, SapLaenderAdapter
adapter-netze/                   client-netze, SapHausanschlussAdapter, SapAbschlagAdapter (Netze)
adapter-odr/                     client-odr, SapTarifAdapter, SapAbschlagAdapter (ODR)
app/                             Spring-Boot-Anwendung, Profile, Integrations- und ArchUnit-Tests
sap-mock/                        WireMock-Attrappe für beide SAP-Systeme
```

Abhängigkeiten gehen nur in eine Richtung: `core` ← `adapter-common` ← `adapter-netze` / `adapter-odr` ← `app`.
`adapter-netze` und `adapter-odr` kennen sich nicht. Gradle erzwingt das beim Kompilieren, ArchUnit
(`app/src/test/.../ArchitekturTest.java`) prüft es zusätzlich.

### Build-Pipeline

```
netze.yaml ─┐                                   (spec-tools:specsAufteilen → spec-tools/build/specs/)
            ├─► Spec-Vergleich ─► common.yaml ──────► Generator ─► de.firma.kuze.sap.common.client  (adapter-common)
odr.yaml  ──┘                 ├─► netze-rest.yaml ──► Generator ─► de.firma.kuze.sap.netze.client   (adapter-netze)
                              └─► odr-rest.yaml ────► Generator ─► de.firma.kuze.sap.odr.client     (adapter-odr)
```

Generierter Code liegt nur unter `<modul>/build/generated/sap-client` und wird nicht eingecheckt.
`KeinGenerierterCodeTest` prüft, dass unter `src/` nichts Generiertes liegt.

## Bauen und Starten

Voraussetzung ist nur Java 21. Gradle kommt über den Wrapper.

```bash
./gradlew build          # kompiliert, generiert, testet alles (entspricht "mvn verify")
```

### Lokal starten über Gradle-Tasks

Für ein lauffähiges System braucht es drei Prozesse, die **in dieser Reihenfolge** gestartet werden.
Jeder läuft dauerhaft in einem eigenen Terminal weiter. Ein vorheriges `build` ist nicht nötig, denn die
Tasks erledigen Spec-Vergleich, Code-Generierung und Kompilieren selbst.

| # | Was | Gradle-Task (VS Code: Gradle-Seitenleiste) | Argumente | Bereit, wenn im Terminal steht |
|---|---|---|---|---|
| 1 | SAP-Mocks Netze (:9081) und ODR (:9082) | `sap-mock` → `application` → `run` | keine | `SAP-Mock NETZE läuft auf http://localhost:9081` |
| 2 | Backend Netze (:8081) | `app` → `application` → `bootRunNetze` | keine | `Started KundenzentrumApplication` |
| 3 | Backend ODR (:8082) | `app` → `application` → `bootRunOdr` | keine | `Started KundenzentrumApplication` |

Wer nur einen Mandanten braucht, startet Schritt 1 und entweder Schritt 2 oder Schritt 3.

**In VS Code** (Erweiterung „Gradle for Java“) startet man alle drei Tasks per Klick, Argumente sind nicht nötig.
Nicht den einfachen Task `bootRun` nehmen: Der startet ohne Profil, und die Anwendung bricht sofort ab.

**Auf der Kommandozeile**, jeweils in einem eigenen Terminal:

```bash
./gradlew :sap-mock:run                                          # 1. beide SAP-Mocks
./gradlew :app:bootRunNetze                                      # 2. Backend Netze :8081
./gradlew :app:bootRunOdr                                        # 3. Backend ODR   :8082
```

`bootRunNetze` und `bootRunOdr` sind in `app/build.gradle.kts` definiert und entsprechen
`./gradlew :app:bootRun --args='--spring.profiles.active=netze'` bzw. `odr`.

Nur einen Mock starten: `./gradlew :sap-mock:run --args=netze` bzw. `--args=odr`.

Beenden mit Ctrl+C im jeweiligen Terminal (VS Code: Papierkorb-Symbol am Terminal). Gradle meldet danach
`BUILD FAILED`, weil der Prozess abgebrochen wurde. Das ist normal.

**Wenn etwas nicht klappt:**

| Symptom | Ursache | Abhilfe |
|---|---|---|
| Postman/curl: `ECONNREFUSED` bzw. `Connection refused` auf 8081/8082 | Backend läuft nicht | Terminal von `bootRun` prüfen, Schritt 2 bzw. 3 erneut starten |
| Terminal: `APPLICATION FAILED TO START` … `Kein gültiger Mandant gesetzt` | `bootRun` ohne Profil gestartet | `bootRunNetze` bzw. `bootRunOdr` verwenden |
| Antwort `502 {"fehler":"SAP_NICHT_VERFUEGBAR"}` bei jeder Anfrage | SAP-Mock läuft nicht | Schritt 1 starten |
| Terminal: `Port 8081 was already in use` | Instanz läuft schon (evtl. in einem anderen Terminal) | alte Instanz beenden oder einfach weiterverwenden |
| VS Code zeigt bei `sap-mock` keine Gruppe `application` oder bei `app` kein `bootRunNetze` | veralteter Projektstand in VS Code | Gradle-Seitenleiste: Aktualisieren-Symbol, sonst Befehl „Java: Reload Projects“ |

### Alternativ: als JAR starten

So läuft es auch ohne Gradle, z. B. auf einem anderen Rechner. Reihenfolge wie oben:

```bash
./gradlew :app:bootJar :sap-mock:installDist                     # einmalig bauen

sap-mock/build/install/sap-mock/bin/sap-mock                      # 1. beide SAP-Mocks (oder: ... sap-mock netze | odr)
java -jar app/build/libs/kundenzentrum-backend.jar --spring.profiles.active=netze   # 2. Backend Netze :8081
java -jar app/build/libs/kundenzentrum-backend.jar --spring.profiles.active=odr     # 3. Backend ODR   :8082
```

### Ausprobieren

Mit curl wie unten oder in Postman einfach `GET` auf dieselben URLs. Es sind keine Header und keine
Anmeldung nötig. Port 8081 ist Netze, 8082 ist ODR.

```bash
curl localhost:8081/api/laender                # [{"code":"DE","name":"Deutschland"}]
curl localhost:8082/api/laender                # [{"code":"DE","name":"Deutschland"}]

curl localhost:8081/api/hausanschluesse/4711   # {"id":"4711","adresse":"Musterweg 1, 12345 Musterstadt","status":"IN_BETRIEB"}
curl -i localhost:8082/api/hausanschluesse/4711   # 404, gibt es bei ODR nicht

curl localhost:8082/api/tarife                 # [{"id":"T1","name":"Strom Basis","grundpreisEuroProMonat":9.90}]
curl -i localhost:8081/api/tarife              # 404, gibt es bei Netze nicht

curl localhost:8081/api/abschlag/VK1           # {"betragEuro":85.00,"naechsteFaelligkeit":"2026-12-01"}
curl localhost:8082/api/abschlag/VK1           # dieselbe Struktur, obwohl ODR in Cent liefert

# Fehlerfälle
curl -i localhost:8081/api/hausanschluesse/9999   # 404 {"fehler":"NICHT_GEFUNDEN"}      (SAP 404)
curl -i localhost:8082/api/abschlag/SAPFEHLER     # 502 {"fehler":"SAP_NICHT_VERFUEGBAR"} (SAP 500)
# Mock beenden, dann:
curl -i localhost:8081/api/laender                # 502 {"fehler":"SAP_NICHT_VERFUEGBAR"} (keine Verbindung)
```

Ohne Profil startet die Anwendung nicht:

```
***************************
APPLICATION FAILED TO START
***************************
Description:
Kein gültiger Mandant gesetzt (kundenzentrum.mandant=null). Das Kundenzentrum braucht beim Start genau einen Mandanten.
Action:
Starte mit einem der Profile "netze" oder "odr", z. B. --spring.profiles.active=netze oder SPRING_PROFILES_ACTIVE=odr.
```

## So funktioniert die Mandanten-Mechanik

- `kundenzentrum.mandant` (`netze` | `odr`) wird in `application-netze.yaml` bzw. `application-odr.yaml` gesetzt,
  zusammen mit `kundenzentrum.sap.base-url`, `user` und `password`.
- `@NurNetze` und `@NurOdr` (in `core/.../mandant`) sind Meta-Annotationen auf `@ConditionalOnProperty`.
  Eine so markierte Bean existiert in der anderen Instanz schlicht nicht. Ein Controller, den es nicht gibt,
  liefert 404 wie jede unbekannte URL.
- `MandantPruefung` (ein `EnvironmentPostProcessor`) bricht vor dem Aufbau des Kontexts ab, wenn kein
  gültiger Mandant gesetzt ist. `MandantFehltAnalyse` formuliert die Meldung.

## So füge ich eine neue Funktion hinzu

Gemeinsam für alle Fälle: Zuerst die Operation in die SAP-Spec(s) unter `specs/sap/` eintragen und
`./gradlew :spec-tools:test -Dspec.bericht.aktualisieren=true` laufen lassen. Dann im Diff von
`specs/sap/vergleich.md` prüfen, in welchem Fall die Operation gelandet ist. Danach weiter wie folgt.

**Fall 1: in beiden Specs identisch (wie `laender`)**
1. `core/.../<feature>/`: `XyzDto`, `XyzPort`, `XyzService`, `XyzController`, alle ohne Mandanten-Annotation.
2. `adapter-common/.../sap/common/<feature>/`: `XyzMapper` (MapStruct, `componentModel = "spring"`)
   und `SapXyzAdapter implements XyzPort`.
3. Die generierte API-Klasse in `SapVerbindungKonfiguration` als Bean anmelden (`new XyzApi(sapCommonApiClient)`).
4. Adapter-Test gegen WireMock (Vorlage: `SapLaenderAdapterTest`) und Profil-Tests in `NetzeProfilTest` und `OdrProfilTest`.

**Fall 2: nur Netze (wie `hausanschluss`)**
1. `core/.../<feature>/`: DTO und Port ohne Annotation, `Service` und `Controller` mit `@NurNetze`.
2. `adapter-netze/.../sap/netze/<feature>/`: Mapper und `SapXyzAdapter` mit `@NurNetze`.
3. API-Bean in `SapNetzeKonfiguration` (die ist schon `@NurNetze`).
4. Tests: Adapter-Test, in `NetzeProfilTest` den Erfolgsfall, in `OdrProfilTest` den 404.

**Fall 3: nur ODR (wie `tarif`)**: wie Fall 2, mit `@NurOdr`, `adapter-odr` und `SapOdrKonfiguration`.
Beträge in Cent rechnet `CentUmrechnung` um (`uses = CentUmrechnung.class`, `qualifiedByName = "centInEuro"`).

**Fall 4: in beiden, aber abweichend (wie `abschlag`)**
1. `core/.../<feature>/`: DTO, Port, Service und Controller, alle ohne Annotation. Der Frontend-Vertrag ist für beide gleich.
2. In `adapter-netze` einen `SapXyzAdapter` mit `@NurNetze`, in `adapter-odr` einen mit `@NurOdr`. Beide implementieren denselben Port.
3. Die Mapper bekommen **mandantenspezifische Namen** (`XyzNetzeMapper`, `XyzOdrMapper`), siehe Abschnitt „Ergebnisse“.
4. API-Beans in `SapNetzeKonfiguration` und `SapOdrKonfiguration` mit unterschiedlichen Bean-Namen (`sapNetzeXyzApi`, `sapOdrXyzApi`).
5. Tests: je ein Adapter-Test und in beiden Profil-Tests dieselbe erwartete Antwort.

## Wenn SAP eine neue Spec liefert

1. Datei unter `specs/sap/netze.yaml` bzw. `odr.yaml` ersetzen. Jede Operation braucht eine `operationId`,
   sonst bricht der Vergleich mit einer Meldung ab.
2. `./gradlew build` ausführen. Ändert sich die Aufteilung, schlägt `BerichtTest` fehl und meldet,
   dass `vergleich.md` nicht mehr passt.
3. `./gradlew :spec-tools:test -Dspec.bericht.aktualisieren=true` schreibt den Bericht neu. Im Diff sieht man,
   welche Operation gewandert ist und warum (der Bericht nennt bei abweichenden Operationen die Stellen).
4. Wandert eine Operation von „gemeinsam“ nach „abweichend“, verschwindet ihre API-Klasse aus
   `client-common`. Der Compiler zeigt dann genau die Adapter in `adapter-common`, die umziehen müssen:
   Fall 1 wird zu Fall 4. Umgekehrt kann man zwei Adapter zu einem in `adapter-common` zusammenlegen.
5. Den neuen Bericht zusammen mit der Spec committen. So steht die Änderung der Aufteilung im Review.

Beim Vergleich zählen HTTP-Methode, Pfad, Parameter, Request-Body und Responses mit vollständig aufgelösten
`$ref`. `description`, `summary`, `example` und `examples` werden ignoriert. Schema-Felder, die so heißen
(z. B. eine Property `description`), bleiben dabei erhalten und werden verglichen.

## Ergebnisse zu den offenen Fragen (Abschnitt 16 des Auftrags)

| Frage | Ergebnis |
|---|---|
| Reihenfolge Spec-Vergleich → Generator, auch bei Einzelbau | Gelöst über Gradle statt Phasen-Reihenfolge: `spec-tools` stellt die Teil-Specs als Artefakt der Konfiguration `teilSpecs` bereit (gebaut von `specsAufteilen`), die Generator-Task im Adapter hat dieses Artefakt als Input. `./gradlew clean :adapter-odr:compileJava` läuft nachweislich `specsAufteilen → sapClientGenerieren → compileJava`. Der Vergleich läuft einmal pro Build statt einmal pro Adapter-Modul und ist cachebar. |
| Konstruktor und Methoden des generierten `ApiClient` (Generator 7.27.0, `restclient`) | `new ApiClient(RestClient)` übernimmt den übergebenen RestClient samt Interceptoren und Status-Handlern. Die Base-URL setzt man mit `setBasePath(String)`, sie wird pro Aufruf vor den Pfad gesetzt (die Base-URL des RestClient wird nicht benutzt). Daneben gibt es `ApiClient()`, `ApiClient(ObjectMapper, DateFormat)` und `ApiClient(RestClient, ObjectMapper, DateFormat)`. Die API-Klassen haben `XyzApi(ApiClient)`. |
| Greift die Fehlerbehandlung am gemeinsamen `RestClient`? | Ja. Der generierte Code ruft `restClient....retrieve()` auf, damit greifen `defaultStatusHandler` (404 und 5xx). Verbindungsfehler fängt ein `ClientHttpRequestInterceptor` ab. Beides sitzt in `SapFehlerUebersetzung`, belegt durch `SapLaenderAdapterTest` (404, 500, Verbindungsfehler, 400 bleibt unübersetzt) und von außen durch `NetzeProfilTest`. |
| Unbenutzte Modellklassen in Teil-Specs | Ja, sie entstehen: `client-common` enthält z. B. `SapHausanschluss` und `SapAbschlag`, `client-netze` enthält `SapLand`. Das stört nicht, denn die Packages sind getrennt, nichts kollidiert und der Compiler-Overhead ist gering. Verwechslungsgefahr ist die einzige Schwäche (siehe offene Punkte). |
| Zwei `SapAbschlagAdapter` mit gleichem Namen | **Die Adapter kollidieren nicht.** `@ConditionalOnProperty` wird beim Component-Scan ausgewertet, bevor der Bean-Name geprüft wird, also wird nur einer registriert. **Die MapStruct-Mapper kollidieren dagegen schon.** Zwei Interfaces `AbschlagMapper` erzeugen zwei `AbschlagMapperImpl` ohne Mandanten-Annotation, das ergab `ConflictingBeanDefinitionException: ... bean name 'abschlagMapperImpl'`. Die Mapper heißen deshalb `AbschlagNetzeMapper` und `AbschlagOdrMapper`. Die Adapter behalten ihren Namen, die Konfigurationen benennen die API-Beans eindeutig (`sapNetzeAbschlagApi`, `sapOdrAbschlagApi`). |

## Abweichungen vom Auftrag

1. **Gradle statt Maven.** Das war eine ausdrückliche Vorgabe nach Beginn der Umsetzung. Daraus folgt:
   - `./gradlew build` entspricht `mvn verify`.
   - Generierter Code liegt unter `build/` statt `target/`.
   - Statt Parent-POM gibt es Version-Catalog `gradle/libs.versions.toml` und Convention-Plugins in `buildSrc`.
   - `openapi-generator-gradle-plugin` statt Maven-Plugin, gleiche Version (7.27.0), gleiche Optionen.
   - Statt `exec-maven-plugin` pro Adapter gibt es eine `JavaExec`-Task in `spec-tools`, deren Ergebnis die Adapter als Artefakt beziehen (siehe oben).
   - Der Bericht-Schalter heißt wie vorgesehen: `-Dspec.bericht.aktualisieren=true`.
2. Das Projekt liegt im Unterordner `kundenzentrum-backend/`, wie in Abschnitt 5 skizziert.
3. Die Mapper für `abschlag` heißen `AbschlagNetzeMapper` und `AbschlagOdrMapper`, weil es sonst eine Bean-Kollision gibt (siehe oben).
4. Die Tags von `getLaender` sind in beiden Specs gleich (`Laender`). Tags werden nicht verglichen,
   bestimmen aber den Namen der generierten API-Klasse. `common.yaml` wird aus der Netze-Spec gebaut
   (Pfade gefiltert, `components` vollständig), Beschreibungen und Beispiele stammen also aus Netze.
5. Zusätzlich zum Auftrag gibt es:
   - einen `FailureAnalyzer` für eine lesbare Startmeldung ohne Mandant;
   - Test-Fixtures in `adapter-common` (`SapTestVerbindung`), damit alle Adapter-Tests über dieselbe echte Verdrahtung laufen;
   - ArchUnit-Regeln über das Geforderte hinaus: keine HTTP-Client-Klassen im Core, keine Mandanten-Annotationen in `adapter-common`, kein Zugriff auf `Mandant` außerhalb des Mandanten-Pakets, generierte Klassen nur unter `de.firma.kuze.sap..`;
   - `KeinGenerierterCodeTest`.

Verwendete Versionen (Stand 16.11.2026): Java 21, Gradle 9.9.1, Spring Boot 3.5.16 (bewusst 3.x, nicht 4.x),
OpenAPI Generator 7.27.0, MapStruct 1.6.3, swagger-parser 2.1.48, WireMock 3.13.2, ArchUnit 1.5.1.

## Offene Punkte

- **Gradle-Deprecation aus dem Generator-Plugin:** `openapi-generator-gradle-plugin` 7.27.0 ruft
  `Configuration.setVisible` auf. Das ist in Gradle 9 deprecated und fällt mit Gradle 11 weg. Vor einem
  Gradle-Upgrade ein neueres Plugin abwarten.
- **Andere SAP-4xx** (400, 401, 403 …) werden nicht übersetzt und kommen beim Frontend als 500 an. Eine
  fachliche Entscheidung steht aus, z. B. 401/403 von SAP als `SAP_NICHT_VERFUEGBAR`.
- **Abbrüche beim Lesen des Antwort-Bodys** (Verbindung reißt nach den Headern ab) werden nicht über den
  Interceptor übersetzt, sondern als `RestClientException` durchgereicht.
- **Keine Timeouts** am `RestClient` konfiguriert. Für den Betrieb nötig.
- **Unbenutzte Modellklassen** könnte `spec-tools` vermeiden, indem es nur die von den Pfaden erreichbaren
  `components` übernimmt. Im Beispiel bewusst nicht umgesetzt (einfachste erlaubte Lösung).
- **Grenzen des Spec-Vergleichs:** Reihenfolgen in Arrays (Parameter, Enum-Werte) zählen. `security`,
  `servers`, `tags`, `deprecated` und Callbacks auf Operationsebene werden nicht verglichen.
- **Zugangsdaten** stehen für das Beispiel im Klartext in den Profilen. Produktiv gehören sie in
  Umgebungsvariablen oder einen Secret-Store (`KUNDENZENTRUM_SAP_PASSWORD`).
- Der SAP-Mock prüft keine Basic-Auth. Ob sie gesendet wird, belegt `SapLaenderAdapterTest`.
