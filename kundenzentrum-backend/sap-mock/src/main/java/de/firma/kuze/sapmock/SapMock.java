package de.firma.kuze.sapmock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

import java.util.List;
import java.util.Locale;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Attrappe für die beiden SAP-Systeme, damit sich beide Backend-Instanzen lokal starten lassen.
 * Die Antworten liegen als WireMock-Mappings unter {@code src/main/resources/sap-mock/<mandant>/mappings}.
 * <p>
 * Aufruf: {@code SapMock [netze|odr]}, ohne Argument starten beide.
 */
public final class SapMock {

    public enum System {
        NETZE(9081), ODR(9082);

        public final int port;

        System(int port) {
            this.port = port;
        }

        String verzeichnis() {
            return "sap-mock/" + name().toLowerCase(Locale.ROOT);
        }
    }

    private SapMock() {
    }

    public static WireMockServer starten(System system, int port) {
        var server = new WireMockServer(wireMockConfig()
                .port(port)
                .usingFilesUnderClasspath(system.verzeichnis())
                .notifier(new ConsoleNotifier(false)));
        server.start();
        return server;
    }

    public static void main(String[] args) {
        List<System> systeme = args.length == 0
                ? List.of(System.values())
                : List.of(System.valueOf(args[0].toUpperCase(Locale.ROOT)));
        for (System system : systeme) {
            var server = starten(system, system.port);
            java.lang.System.out.printf("SAP-Mock %s läuft auf %s%n", system, server.baseUrl());
        }
    }
}
