package de.firma.kuze;

import de.firma.kuze.laender.LaenderPort;
import de.firma.kuze.mandant.Mandant;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("odr")
class OdrProfilTest extends ProfilTestBasis {

    @Autowired
    ApplicationContext kontext;

    @Test
    void kontextStartetFuerOdr() {
        assertThat(kontext.getEnvironment().getProperty(Mandant.PROPERTY)).isEqualTo("odr");
        assertThat(kontext.getBean(LaenderPort.class)).isNotNull();
    }

    @Test
    void laender() throws Exception {
        SAP.stubFor(WireMock.get("/laender").willReturn(okJson("[{\"landKz\":\"AT\",\"bezeichnung\":\"Österreich\"}]")));

        mockMvc.perform(get("/api/laender"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"code\":\"AT\",\"name\":\"Österreich\"}]", JsonCompareMode.STRICT));
    }

    @Test
    void hausanschlussGibtEsInOdrNicht() throws Exception {
        mockMvc.perform(get("/api/hausanschluesse/4711")).andExpect(status().isNotFound());
        SAP.verify(0, WireMock.anyRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void tarife() throws Exception {
        SAP.stubFor(WireMock.get("/tarife").willReturn(okJson("""
                [{"tarifId":"T1","tarifBez":"Strom Basis","grundpreisCent":990}]""")));

        mockMvc.perform(get("/api/tarife"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"id\":\"T1\",\"name\":\"Strom Basis\",\"grundpreisEuroProMonat\":9.90}]", JsonCompareMode.STRICT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("9.90")));
    }

    @Test
    void abschlagHatDieselbeStrukturWieInDerAnderenInstanz() throws Exception {
        SAP.stubFor(WireMock.get("/abschlaege/VK1").willReturn(okJson("""
                {"betragCent":8500,"faelligkeit":"2026-12-01","rhythmus":"MONATLICH"}""")));

        mockMvc.perform(get("/api/abschlag/VK1"))
                .andExpect(status().isOk())
                .andExpect(content().json(ERWARTETER_ABSCHLAG, JsonCompareMode.STRICT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("85.00")));
    }
}
