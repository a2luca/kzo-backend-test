package de.firma.kuze.sap.common;

import de.firma.kuze.fehler.NichtGefundenException;
import de.firma.kuze.fehler.SapNichtVerfuegbarException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;

/**
 * Die eine Stelle, an der SAP-Fehler in Core-Exceptions übersetzt werden. Sitzt am gemeinsamen
 * {@link RestClient}, den alle generierten {@code ApiClient}s benutzen:
 * <ul>
 *     <li>SAP 404 → {@link NichtGefundenException}</li>
 *     <li>SAP 5xx → {@link SapNichtVerfuegbarException}</li>
 *     <li>Verbindungsfehler (keine Antwort) → {@link SapNichtVerfuegbarException}</li>
 * </ul>
 */
public final class SapFehlerUebersetzung {

    private SapFehlerUebersetzung() {
    }

    public static RestClient.Builder anwenden(RestClient.Builder builder) {
        return builder
                .requestInterceptor(SapFehlerUebersetzung::verbindungsfehlerUebersetzen)
                .defaultStatusHandler(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (anfrage, antwort) -> {
                    throw new NichtGefundenException("SAP 404 für " + beschreibung(anfrage), null);
                })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (anfrage, antwort) -> {
                    throw new SapNichtVerfuegbarException(
                            "SAP " + antwort.getStatusCode().value() + " für " + beschreibung(anfrage), null);
                });
    }

    private static ClientHttpResponse verbindungsfehlerUebersetzen(
            HttpRequest anfrage, byte[] body, ClientHttpRequestExecution ausfuehrung) {
        try {
            return ausfuehrung.execute(anfrage, body);
        } catch (IOException e) {
            throw new SapNichtVerfuegbarException("SAP nicht erreichbar für " + beschreibung(anfrage), e);
        }
    }

    private static String beschreibung(HttpRequest anfrage) {
        return anfrage.getMethod() + " " + anfrage.getURI();
    }
}
