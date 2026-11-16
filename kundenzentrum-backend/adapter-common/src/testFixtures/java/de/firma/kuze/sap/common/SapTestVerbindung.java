package de.firma.kuze.sap.common;

import org.springframework.web.client.RestClient;

/** Baut die SAP-Verbindung genau wie in der Anwendung, aber ohne Spring-Kontext. Für Adapter-Tests gegen WireMock. */
public final class SapTestVerbindung {

    public static final String USER = "kuze";
    public static final String PASSWORT = "geheim";

    private SapTestVerbindung() {
    }

    public static SapProperties properties(String baseUrl) {
        return new SapProperties(baseUrl, USER, PASSWORT);
    }

    public static RestClient restClient(String baseUrl) {
        return new SapVerbindungKonfiguration().sapRestClient(RestClient.builder(), properties(baseUrl));
    }
}
