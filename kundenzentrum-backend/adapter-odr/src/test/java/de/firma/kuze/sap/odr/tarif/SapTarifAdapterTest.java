package de.firma.kuze.sap.odr.tarif;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.firma.kuze.fehler.SapNichtVerfuegbarException;
import de.firma.kuze.sap.common.SapTestVerbindung;
import de.firma.kuze.sap.odr.SapOdrKonfiguration;
import de.firma.kuze.tarif.TarifDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SapTarifAdapterTest {

    @RegisterExtension
    static WireMockExtension sap = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private SapTarifAdapter adapter() {
        var konfiguration = new SapOdrKonfiguration();
        var apiClient = konfiguration.sapOdrApiClient(
                SapTestVerbindung.restClient(sap.baseUrl()), SapTestVerbindung.properties(sap.baseUrl()));
        return new SapTarifAdapter(konfiguration.sapTarifApi(apiClient), Mappers.getMapper(TarifMapper.class));
    }

    @Test
    void rechnetCentInEuroUm() {
        sap.stubFor(get("/tarife").willReturn(okJson("""
                [{"tarifId":"T1","tarifBez":"Strom Basis","grundpreisCent":990},
                 {"tarifId":"T2","tarifBez":"Strom Öko","grundpreisCent":1250}]""")));

        assertThat(adapter().alleTarife()).containsExactly(
                new TarifDto("T1", "Strom Basis", new BigDecimal("9.90")),
                new TarifDto("T2", "Strom Öko", new BigDecimal("12.50")));
    }

    @Test
    void sap503WirdSapNichtVerfuegbar() {
        sap.stubFor(get("/tarife").willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> adapter().alleTarife()).isInstanceOf(SapNichtVerfuegbarException.class);
    }
}
