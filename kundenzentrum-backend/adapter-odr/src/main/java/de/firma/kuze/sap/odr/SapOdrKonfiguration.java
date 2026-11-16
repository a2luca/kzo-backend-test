package de.firma.kuze.sap.odr;

import de.firma.kuze.mandant.NurOdr;
import de.firma.kuze.sap.common.SapProperties;
import de.firma.kuze.sap.odr.client.ApiClient;
import de.firma.kuze.sap.odr.client.api.AbschlagApi;
import de.firma.kuze.sap.odr.client.api.TarifApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Der generierte ODR-Client. Benutzt den gemeinsamen RestClient und damit auch dessen Fehlerübersetzung. */
@NurOdr
@Configuration(proxyBeanMethods = false)
public class SapOdrKonfiguration {

    @Bean
    public ApiClient sapOdrApiClient(RestClient sapRestClient, SapProperties properties) {
        return new ApiClient(sapRestClient).setBasePath(properties.baseUrl());
    }

    @Bean
    public TarifApi sapTarifApi(ApiClient sapOdrApiClient) {
        return new TarifApi(sapOdrApiClient);
    }

    @Bean
    public AbschlagApi sapOdrAbschlagApi(ApiClient sapOdrApiClient) {
        return new AbschlagApi(sapOdrApiClient);
    }
}
