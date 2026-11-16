package de.firma.kuze.sap.odr.abschlag;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.firma.kuze.abschlag.AbschlagDto;
import de.firma.kuze.fehler.NichtGefundenException;
import de.firma.kuze.sap.common.SapTestVerbindung;
import de.firma.kuze.sap.odr.SapOdrKonfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SapAbschlagAdapterTest {

    @RegisterExtension
    static WireMockExtension sap = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private SapAbschlagAdapter adapter() {
        var konfiguration = new SapOdrKonfiguration();
        var apiClient = konfiguration.sapOdrApiClient(
                SapTestVerbindung.restClient(sap.baseUrl()), SapTestVerbindung.properties(sap.baseUrl()));
        return new SapAbschlagAdapter(
                konfiguration.sapOdrAbschlagApi(apiClient), Mappers.getMapper(AbschlagOdrMapper.class));
    }

    @Test
    void rechnetCentInEuroUmUndLaesstRhythmusWeg() {
        sap.stubFor(get("/abschlaege/VK1").willReturn(okJson("""
                {"betragCent":8500,"faelligkeit":"2026-12-01","rhythmus":"MONATLICH"}""")));

        assertThat(adapter().abschlag("VK1"))
                .isEqualTo(new AbschlagDto(new BigDecimal("85.00"), LocalDate.of(2026, 12, 1)));
    }

    @Test
    void sap404WirdNichtGefunden() {
        sap.stubFor(get("/abschlaege/X").willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> adapter().abschlag("X")).isInstanceOf(NichtGefundenException.class);
    }
}
