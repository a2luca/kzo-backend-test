package de.firma.kuze.sapmock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Prüft, dass die Mappings geladen werden und die Beispielwerte aus Abschnitt 7 liefern. */
class SapMockTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "NETZE | /laender               | 200 | \"landKz\":\"DE\"",
            "NETZE | /hausanschluesse/4711  | 200 | \"statusKz\":\"B\"",
            "NETZE | /hausanschluesse/1     | 404 | ''",
            "NETZE | /abschlaege/VK1        | 200 | \"faelligAm\":\"2026-12-01\"",
            "NETZE | /abschlaege/SAPFEHLER  | 500 | ''",
            "NETZE | /tarife                | 404 | ''",
            "ODR   | /laender               | 200 | \"bezeichnung\":\"Deutschland\"",
            "ODR   | /tarife                | 200 | \"grundpreisCent\":990",
            "ODR   | /abschlaege/VK1        | 200 | \"betragCent\":8500",
            "ODR   | /abschlaege/UNBEKANNT  | 404 | ''",
            "ODR   | /hausanschluesse/4711  | 404 | ''",
    })
    void liefertBeispielwerte(SapMock.System system, String pfad, int status, String enthaelt) throws Exception {
        var server = SapMock.starten(system, 0);
        try (var client = HttpClient.newHttpClient()) {
            var antwort = client.send(HttpRequest.newBuilder(URI.create(server.baseUrl() + pfad)).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(antwort.statusCode()).isEqualTo(status);
            assertThat(antwort.body().replace(" ", "")).contains(enthaelt);
        } finally {
            server.stop();
        }
    }
}
