# Spec-Vergleich Netze / ODR

Erzeugt von `spec-tools` aus `netze.yaml` und `odr.yaml`. Nicht von Hand bearbeiten,
sondern mit `./gradlew :spec-tools:test -Dspec.bericht.aktualisieren=true` neu schreiben.

## Gemeinsam (→ `common.yaml`)

- `getLaender` – GET /laender

## Nur auf einer Seite

- `getHausanschluss` – GET /hausanschluesse/{anschlussNr} – nur Netze (→ `netze-rest.yaml`)
- `getTarife` – GET /tarife – nur ODR (→ `odr-rest.yaml`)

## In beiden, aber abweichend (→ `netze-rest.yaml` und `odr-rest.yaml`)

### `getAbschlag`

Netze: GET /abschlaege/{vertragskonto}, ODR: GET /abschlaege/{vertragskonto}

- `responses › 200 › content › application/json › schema › properties`: nur in Netze `betrag`, `faelligAm`; nur in ODR `betragCent`, `faelligkeit`, `rhythmus`
- `responses › 200 › content › application/json › schema › required`: Netze `["betrag","faelligAm"]`, ODR `["betragCent","faelligkeit","rhythmus"]`
