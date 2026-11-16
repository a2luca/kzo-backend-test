package de.firma.kuze.sap.netze.hausanschluss;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.firma.kuze.fehler.NichtGefundenException;
import de.firma.kuze.hausanschluss.HausanschlussDto;
import de.firma.kuze.hausanschluss.HausanschlussStatus;
import de.firma.kuze.sap.common.SapTestVerbindung;
import de.firma.kuze.sap.netze.SapNetzeKonfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SapHausanschlussAdapterTest {

    @RegisterExtension
    static WireMockExtension sap = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private SapHausanschlussAdapter adapter() {
        var konfiguration = new SapNetzeKonfiguration();
        var apiClient = konfiguration.sapNetzeApiClient(
                SapTestVerbindung.restClient(sap.baseUrl()), SapTestVerbindung.properties(sap.baseUrl()));
        return new SapHausanschlussAdapter(
                konfiguration.sapHausanschlussApi(apiClient), Mappers.getMapper(HausanschlussMapper.class));
    }

    @Test
    void setztAdresseZusammenUndUebersetztStatus() {
        sap.stubFor(get("/hausanschluesse/4711").willReturn(okJson("""
                {"anschlussNr":"4711","strasse":"Musterweg","hausNr":"1","plz":"12345","ort":"Musterstadt","statusKz":"B"}""")));

        assertThat(adapter().hausanschluss("4711")).isEqualTo(
                new HausanschlussDto("4711", "Musterweg 1, 12345 Musterstadt", HausanschlussStatus.IN_BETRIEB));
    }

    @ParameterizedTest
    @CsvSource({"B, IN_BETRIEB", "P, IN_PLANUNG", "S, STILLGELEGT"})
    void statuskennzeichen(String kennzeichen, HausanschlussStatus erwartet) {
        sap.stubFor(get("/hausanschluesse/1").willReturn(okJson("""
                {"anschlussNr":"1","strasse":"A","hausNr":"2","plz":"3","ort":"B","statusKz":"%s"}""".formatted(kennzeichen))));

        assertThat(adapter().hausanschluss("1").status()).isEqualTo(erwartet);
    }

    @Test
    void sap404WirdNichtGefunden() {
        sap.stubFor(get("/hausanschluesse/0").willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> adapter().hausanschluss("0")).isInstanceOf(NichtGefundenException.class);
    }
}
