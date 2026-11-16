package de.firma.kuze;

import de.firma.kuze.laender.LaenderPort;
import de.firma.kuze.mandant.Mandant;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("netze")
class NetzeProfilTest extends ProfilTestBasis {

    @Autowired
    ApplicationContext kontext;

    @Test
    void kontextStartetFuerNetze() {
        assertThat(kontext.getEnvironment().getProperty(Mandant.PROPERTY)).isEqualTo("netze");
        assertThat(kontext.getBean(LaenderPort.class)).isNotNull();
    }

    @Test
    void laender() throws Exception {
        SAP.stubFor(WireMock.get("/laender").willReturn(okJson("[{\"landKz\":\"DE\",\"bezeichnung\":\"Deutschland\"}]")));

        mockMvc.perform(get("/api/laender"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"code\":\"DE\",\"name\":\"Deutschland\"}]", JsonCompareMode.STRICT));
    }

    @Test
    void sap404WirdVonAussen404() throws Exception {
        SAP.stubFor(WireMock.get("/laender").willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get("/api/laender"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"fehler\":\"NICHT_GEFUNDEN\"}", JsonCompareMode.STRICT));
    }

    @Test
    void sap500WirdVonAussen502() throws Exception {
        SAP.stubFor(WireMock.get("/laender").willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/api/laender"))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("{\"fehler\":\"SAP_NICHT_VERFUEGBAR\"}", JsonCompareMode.STRICT));
    }

    @Test
    void hausanschluss() throws Exception {
        SAP.stubFor(WireMock.get("/hausanschluesse/4711").willReturn(okJson("""
                {"anschlussNr":"4711","strasse":"Musterweg","hausNr":"1","plz":"12345","ort":"Musterstadt","statusKz":"B"}""")));

        mockMvc.perform(get("/api/hausanschluesse/4711"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"id\":\"4711\",\"adresse\":\"Musterweg 1, 12345 Musterstadt\",\"status\":\"IN_BETRIEB\"}", JsonCompareMode.STRICT));
    }

    @Test
    void tarifeGibtEsInNetzeNicht() throws Exception {
        mockMvc.perform(get("/api/tarife")).andExpect(status().isNotFound());
        SAP.verify(0, WireMock.anyRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void abschlagHatDieselbeStrukturWieInDerAnderenInstanz() throws Exception {
        SAP.stubFor(WireMock.get("/abschlaege/VK1").willReturn(okJson("""
                {"betrag":85.00,"faelligAm":"2026-12-01"}""")));

        mockMvc.perform(get("/api/abschlag/VK1"))
                .andExpect(status().isOk())
                .andExpect(content().json(ERWARTETER_ABSCHLAG, JsonCompareMode.STRICT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("85.00")));
    }
}
