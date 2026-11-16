package de.firma.kuze.sap.netze.abschlag;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.firma.kuze.abschlag.AbschlagDto;
import de.firma.kuze.sap.common.SapTestVerbindung;
import de.firma.kuze.sap.netze.SapNetzeKonfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class SapAbschlagAdapterTest {

    @RegisterExtension
    static WireMockExtension sap = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private SapAbschlagAdapter adapter() {
        var konfiguration = new SapNetzeKonfiguration();
        var apiClient = konfiguration.sapNetzeApiClient(
                SapTestVerbindung.restClient(sap.baseUrl()), SapTestVerbindung.properties(sap.baseUrl()));
        return new SapAbschlagAdapter(
                konfiguration.sapNetzeAbschlagApi(apiClient), Mappers.getMapper(AbschlagNetzeMapper.class));
    }

    @Test
    void mapptEuroBetragUndFaelligkeit() {
        sap.stubFor(get("/abschlaege/VK1").willReturn(okJson("""
                {"betrag":85.00,"faelligAm":"2026-12-01"}""")));

        assertThat(adapter().abschlag("VK1"))
                .isEqualTo(new AbschlagDto(new BigDecimal("85.00"), LocalDate.of(2026, 12, 1)));
    }

    @Test
    void rundetAufZweiNachkommastellen() {
        sap.stubFor(get("/abschlaege/VK2").willReturn(okJson("""
                {"betrag":42.5,"faelligAm":"2027-01-15"}""")));

        assertThat(adapter().abschlag("VK2").betragEuro()).isEqualByComparingTo("42.50").hasToString("42.50");
    }
}
