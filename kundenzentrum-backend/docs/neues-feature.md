# Ein neues Feature umsetzen

Diese Anleitung führt Schritt für Schritt durch ein neues Feature, also einen neuen REST-Endpoint für das
Frontend, der Daten aus SAP holt. Welcher der vier Fälle vorliegt, entscheidet die SAP-Spec, nicht das
Bauchgefühl. Wie die Clients aus den Specs entstehen, steht in [sap-clients-generieren.md](sap-clients-generieren.md).

## Überblick: die vier Fälle

| Fall | Erkennbar in `specs/sap/vergleich.md` unter | SAP-Adapter in | Annotationen | Vorlage im Code |
|---|---|---|---|---|
| 1. identisch | „Gemeinsam“ | `adapter-common` | keine | `laender` |
| 2. nur Netze | „Nur auf einer Seite … nur Netze“ | `adapter-netze` | `@NurNetze` auf Controller, Service, Adapter | `hausanschluss` |
| 3. nur ODR | „Nur auf einer Seite … nur ODR“ | `adapter-odr` | `@NurOdr` auf Controller, Service, Adapter | `tarif` |
| 4. abweichend | „In beiden, aber abweichend“ | `adapter-netze` **und** `adapter-odr` | nur die beiden Adapter | `abschlag` |

Für jedes Feature entstehen dieselben Bausteine:

```
core/src/main/java/de/firma/kuze/<feature>/
├── XyzDto.java          Frontend-Vertrag (Record), unabhängig von SAP
├── XyzPort.java         Interface, das der Adapter implementiert
├── XyzService.java      Fachlogik, ruft den Port
└── XyzController.java   REST-Endpoint

adapter-<common|netze|odr>/src/main/java/de/firma/kuze/sap/<common|netze|odr>/<feature>/
├── XyzMapper.java       MapStruct: generierte SapXyz-Klasse → XyzDto
└── SapXyzAdapter.java   implementiert XyzPort, ruft die generierte XyzApi
```

## Schritt 0: Spec ergänzen und Fall bestimmen

1. Die neue Operation in `specs/sap/netze.yaml` und/oder `odr.yaml` eintragen, jeweils mit `operationId`
   und `tags`. Bei einer Operation, die es in beiden Systemen gibt, muss die `operationId` gleich sein.
2. Bericht neu schreiben und Fall ablesen:
   ```bash
   ./gradlew :spec-tools:test -Dspec.bericht.aktualisieren=true
   ```
   In `specs/sap/vergleich.md` steht die Operation jetzt unter genau einer der drei Überschriften.
3. Clients generieren: `./gradlew compileJava`, danach in VS Code „Java: Reload Projects“.
4. Die generierte API-Klasse und das Modell ansehen (unter `<adapter>/build/generated/sap-client/...`),
   z. B. `ZaehlerApi.getZaehlerstand(String)` und `SapZaehlerstand`.

## Fall 1: in beiden Specs identisch

Durchgespieltes Beispiel: `GET /api/zaehlerstaende/{zaehlerId}`. Die SAP-Operation `getZaehlerstand`
(Tag `Zaehler`) liefert `{ "zaehlerNr": "Z1", "stand": 1234, "ablesedatum": "2026-10-01" }`.

**Core: DTO, Port, Service, Controller** (`core/.../zaehlerstand/`, alles ohne Mandanten-Annotation):

```java
public record ZaehlerstandDto(String zaehlerId, long stand, LocalDate abgelesenAm) {
}
```

```java
public interface ZaehlerstandPort {

    ZaehlerstandDto zaehlerstand(String zaehlerId);
}
```

```java
@Service
public class ZaehlerstandService {

    private final ZaehlerstandPort port;

    public ZaehlerstandService(ZaehlerstandPort port) {
        this.port = port;
    }

    public ZaehlerstandDto zaehlerstand(String zaehlerId) {
        return port.zaehlerstand(zaehlerId);
    }
}
```

```java
@RestController
@RequestMapping("/api/zaehlerstaende")
public class ZaehlerstandController {

    private final ZaehlerstandService service;

    public ZaehlerstandController(ZaehlerstandService service) {
        this.service = service;
    }

    @GetMapping("/{zaehlerId}")
    public ZaehlerstandDto zaehlerstand(@PathVariable String zaehlerId) {
        return service.zaehlerstand(zaehlerId);
    }
}
```

Regeln für den Core: keine Imports aus `de.firma.kuze.sap..`, kein `RestClient`, keine SAP-Feldnamen im DTO.
Feldnamen und Pfade richten sich nach dem Frontend.

**Adapter: API-Bean anmelden** in `adapter-common/.../sap/common/SapVerbindungKonfiguration.java`:

```java
@Bean
public ZaehlerApi sapZaehlerApi(ApiClient sapCommonApiClient) {
    return new ZaehlerApi(sapCommonApiClient);
}
```

**Adapter: Mapper und Adapter** (`adapter-common/.../sap/common/zaehlerstand/`):

```java
@Mapper(componentModel = "spring")
public interface ZaehlerstandMapper {

    @Mapping(target = "zaehlerId", source = "zaehlerNr")
    @Mapping(target = "abgelesenAm", source = "ablesedatum")
    ZaehlerstandDto zuDto(SapZaehlerstand zaehlerstand);
}
```

```java
@Component
public class SapZaehlerstandAdapter implements ZaehlerstandPort {

    private final ZaehlerApi api;
    private final ZaehlerstandMapper mapper;

    public SapZaehlerstandAdapter(ZaehlerApi api, ZaehlerstandMapper mapper) {
        this.api = api;
        this.mapper = mapper;
    }

    @Override
    public ZaehlerstandDto zaehlerstand(String zaehlerId) {
        return mapper.zuDto(api.getZaehlerstand(zaehlerId));
    }
}
```

Fehlerbehandlung muss man nicht selbst bauen: SAP-404 wird automatisch zu `NichtGefundenException`
(Frontend: 404), SAP-5xx und Verbindungsfehler zu `SapNichtVerfuegbarException` (Frontend: 502).
Das erledigt `SapFehlerUebersetzung` am gemeinsamen `RestClient`.

**MapStruct-Hinweise:**
- Ein Zielfeld, das nicht gemappt wird, bricht den Build ab (`unmappedTargetPolicy=ERROR`). Das ist gewollt.
- Gleichnamige Felder mappt MapStruct automatisch, abweichende per `@Mapping(target, source)`.
- Umrechnungen als `default`-Methode im Mapper (Beispiel: `adresse(...)` in `HausanschlussMapper`) oder als
  gemeinsame Hilfsklasse über `uses` (Beispiel: `CentUmrechnung` in `adapter-odr`).
- SAP-Kennzeichen auf ein Core-Enum mit `@ValueMapping` (Beispiel: `HausanschlussMapper.status`).

**Tests:**
1. Adapter-Test gegen WireMock in `adapter-common/src/test/...`. Vorlage: `SapLaenderAdapterTest`. Er
   steckt eine SAP-Beispielantwort hinein und prüft das DTO. `SapTestVerbindung` liefert die echte
   Verdrahtung samt Fehlerübersetzung.
2. Profil-Tests in `app/src/test/.../NetzeProfilTest.java` **und** `OdrProfilTest.java`: SAP-Antwort
   stubben, Endpoint per MockMvc aufrufen, JSON prüfen (`JsonCompareMode.STRICT`).

## Fall 2: nur Netze

Wie Fall 1, mit diesen Unterschieden:

| Baustein | Unterschied |
|---|---|
| `XyzDto`, `XyzPort` | unverändert, **ohne** Annotation |
| `XyzService`, `XyzController` | zusätzlich `@NurNetze` |
| API-Bean | in `adapter-netze/.../SapNetzeKonfiguration.java` (die Klasse ist schon `@NurNetze`), Parameter `ApiClient sapNetzeApiClient` |
| Mapper, Adapter | in `adapter-netze/.../sap/netze/<feature>/`, der Adapter mit `@NurNetze` |
| Imports | generierte Klassen aus `de.firma.kuze.sap.netze.client...` |

```java
@NurNetze
@RestController
@RequestMapping("/api/xyz")
public class XyzController { ... }
```

```java
@NurNetze
@Component
public class SapXyzAdapter implements XyzPort { ... }
```

**Tests:** Adapter-Test in `adapter-netze` (Vorlage `SapHausanschlussAdapterTest`). In `NetzeProfilTest`
den Erfolgsfall prüfen, in `OdrProfilTest` den 404 (Vorlage `hausanschlussGibtEsInOdrNicht`). Der Endpoint
existiert in der ODR-Instanz nicht, deshalb kommt der übliche 404 für unbekannte URLs.

## Fall 3: nur ODR

Spiegelbildlich zu Fall 2: `@NurOdr`, `adapter-odr`, `SapOdrKonfiguration` mit `ApiClient sapOdrApiClient`,
Imports aus `de.firma.kuze.sap.odr.client...`. Vorlage: `tarif`. Beträge in Cent rechnet `CentUmrechnung`
in Euro um:

```java
@Mapper(componentModel = "spring", uses = CentUmrechnung.class)
public interface XyzMapper {

    @Mapping(target = "betragEuro", source = "betragCent", qualifiedByName = "centInEuro")
    XyzDto zuDto(SapXyz xyz);
}
```

**Tests:** Adapter-Test in `adapter-odr`, Erfolgsfall in `OdrProfilTest`, 404 in `NetzeProfilTest`.

## Fall 4: in beiden, aber abweichend

Das Frontend bekommt in beiden Instanzen dieselbe Antwort, nur die SAP-Seite unterscheidet sich.

| Baustein | Wo | Annotation |
|---|---|---|
| `XyzDto`, `XyzPort`, `XyzService`, `XyzController` | `core` | **keine** |
| API-Bean `sapNetzeXyzApi` | `SapNetzeKonfiguration` | (Klasse ist `@NurNetze`) |
| API-Bean `sapOdrXyzApi` | `SapOdrKonfiguration` | (Klasse ist `@NurOdr`) |
| `XyzNetzeMapper`, `SapXyzAdapter` | `adapter-netze/.../sap/netze/<feature>/` | Adapter `@NurNetze` |
| `XyzOdrMapper`, `SapXyzAdapter` | `adapter-odr/.../sap/odr/<feature>/` | Adapter `@NurOdr` |

**Wichtig bei den Namen:**
- **Mapper brauchen unterschiedliche Namen** (`XyzNetzeMapper`, `XyzOdrMapper`). Die von MapStruct
  erzeugten `…MapperImpl`-Klassen tragen keine Mandanten-Annotation. Zwei gleichnamige Mapper ergäben
  denselben Bean-Namen, und der Start bricht mit `ConflictingBeanDefinitionException` ab.
- **API-Bean-Methoden mandantenspezifisch benennen** (`sapNetzeXyzApi`, `sapOdrXyzApi`).
- **Die Adapter dürfen gleich heißen** (`SapXyzAdapter`). Durch `@NurNetze`/`@NurOdr` wird immer nur
  einer registriert.

Felder, die nur eine Seite liefert und die das Frontend nicht braucht, lässt der Mapper einfach weg
(Beispiel: `rhythmus` in `AbschlagOdrMapper`). Fehlt dagegen auf einer Seite ein Feld, das das Frontend
braucht, muss das fachlich geklärt werden. Im Zweifel gehört dann das DTO angepasst, nicht der Vertrag
gebogen.

**Tests:** je ein Adapter-Test in `adapter-netze` und `adapter-odr`. In **beiden** Profil-Tests dieselbe
erwartete Antwort prüfen, am besten als gemeinsame Konstante in `ProfilTestBasis` (Vorlage:
`ERWARTETER_ABSCHLAG`).

## Abschluss-Checkliste

- [ ] Operation in der/den Spec(s) mit `operationId` und `tags`
- [ ] `specs/sap/vergleich.md` neu geschrieben, Fall passt zur Erwartung
- [ ] Core: DTO, Port, Service, Controller, Annotationen je nach Fall
- [ ] Adapter: API-Bean in der passenden Konfiguration, Mapper, `SapXyzAdapter`
- [ ] Fall 4: Mapper-Namen mit `Netze`/`Odr`, API-Bean-Namen mit `sapNetze…`/`sapOdr…`
- [ ] Adapter-Test(s) gegen WireMock
- [ ] Profil-Tests: Erfolgsfall im passenden Profil, 404 im anderen (Fall 2/3) bzw. gleiche Antwort in beiden (Fall 1/4)
- [ ] Optional: Antwort im `sap-mock` ergänzen (`sap-mock/src/main/resources/sap-mock/<netze|odr>/mappings/`), damit der Endpoint lokal funktioniert
- [ ] `./gradlew build` grün (inkl. `ArchitekturTest`, `BerichtTest`, `KeinGenerierterCodeTest`)
- [ ] Spec, `vergleich.md` und Code zusammen committen

## Was ArchUnit beim Build automatisch prüft

`app/src/test/java/de/firma/kuze/ArchitekturTest.java` bricht den Build ab, wenn:
- der Core etwas aus `de.firma.kuze.sap..` oder `org.springframework.web.client..` benutzt,
- `adapter-netze` auf `de.firma.kuze.sap.odr..` zugreift oder umgekehrt,
- `adapter-common` Netze-/ODR-Klassen benutzt oder `@NurNetze`/`@NurOdr` trägt,
- irgendwo außerhalb von `de.firma.kuze.mandant` das Enum `Mandant` benutzt wird (kein `if (mandant == ...)`),
- generierte Client-Klassen außerhalb von `de.firma.kuze.sap..` verwendet werden.
