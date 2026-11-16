package de.firma.kuze.sap.common.laender;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.firma.kuze.fehler.NichtGefundenException;
import de.firma.kuze.fehler.SapNichtVerfuegbarException;
import de.firma.kuze.laender.LandDto;
import de.firma.kuze.sap.common.SapTestVerbindung;
import de.firma.kuze.sap.common.SapVerbindungKonfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mapstruct.factory.Mappers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SapLaenderAdapterTest {

    @RegisterExtension
    static WireMockExtension sap = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static SapLaenderAdapter adapter(String baseUrl) {
        var konfiguration = new SapVerbindungKonfiguration();
        var properties = SapTestVerbindung.properties(baseUrl);
        var apiClient = konfiguration.sapCommonApiClient(SapTestVerbindung.restClient(baseUrl), properties);
        return new SapLaenderAdapter(konfiguration.sapLaenderApi(apiClient), Mappers.getMapper(LaenderMapper.class));
    }

    @Test
    void mapptSapAntwortAufFrontendDto() {
        sap.stubFor(get("/laender").willReturn(okJson("""
                [{"landKz":"DE","bezeichnung":"Deutschland"},{"landKz":"AT","bezeichnung":"Österreich"}]""")));

        var laender = adapter(sap.baseUrl()).alleLaender();

        assertThat(laender).containsExactly(new LandDto("DE", "Deutschland"), new LandDto("AT", "Österreich"));
        sap.verify(getRequestedFor(urlEqualTo("/laender"))
                .withBasicAuth(new com.github.tomakehurst.wiremock.client.BasicCredentials(
                        SapTestVerbindung.USER, SapTestVerbindung.PASSWORT)));
    }

    @Test
    void sap404WirdNichtGefunden() {
        sap.stubFor(get("/laender").willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> adapter(sap.baseUrl()).alleLaender())
                .isInstanceOf(NichtGefundenException.class)
                .hasMessageContaining("GET").hasMessageContaining("/laender");
    }

    @Test
    void sap500WirdSapNichtVerfuegbar() {
        sap.stubFor(get("/laender").willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> adapter(sap.baseUrl()).alleLaender())
                .isInstanceOf(SapNichtVerfuegbarException.class)
                .hasMessageContaining("SAP 500");
    }

    @Test
    void verbindungsfehlerWirdSapNichtVerfuegbar() {
        // Port 9 (discard) ist lokal praktisch nie offen: Verbindung wird abgelehnt.
        assertThatThrownBy(() -> adapter("http://localhost:9").alleLaender())
                .isInstanceOf(SapNichtVerfuegbarException.class)
                .hasMessageContaining("nicht erreichbar");
    }

    @Test
    void sap400BleibtUnuebersetzt() {
        sap.stubFor(get("/laender").willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> adapter(sap.baseUrl()).alleLaender())
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.BadRequest.class);
    }
}
