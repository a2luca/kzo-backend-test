package de.firma.kuze;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Startet die ganze Anwendung im Profil der Unterklasse gegen ein WireMock-SAP.
 * Der WireMock-Server lebt so lange wie die Test-JVM, damit gecachte Spring-Kontexte ihre Base-URL behalten.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ProfilTestBasis {

    /** Frontend-Vertrag für GET /api/abschlag/{id}, gleich für beide Mandanten. */
    static final String ERWARTETER_ABSCHLAG = "{\"betragEuro\":85.00,\"naechsteFaelligkeit\":\"2026-12-01\"}";

    static final WireMockServer SAP = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        SAP.start();
    }

    @DynamicPropertySource
    static void sapAufWireMock(DynamicPropertyRegistry registry) {
        registry.add("kundenzentrum.sap.base-url", SAP::baseUrl);
    }

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void sapZuruecksetzen() {
        SAP.resetAll();
    }
}
