package de.firma.kuze.sap.netze;

import de.firma.kuze.mandant.NurNetze;
import de.firma.kuze.sap.common.SapProperties;
import de.firma.kuze.sap.netze.client.ApiClient;
import de.firma.kuze.sap.netze.client.api.AbschlagApi;
import de.firma.kuze.sap.netze.client.api.HausanschlussApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Der generierte Netze-Client. Benutzt den gemeinsamen RestClient und damit auch dessen Fehlerübersetzung. */
@NurNetze
@Configuration(proxyBeanMethods = false)
public class SapNetzeKonfiguration {

    @Bean
    public ApiClient sapNetzeApiClient(RestClient sapRestClient, SapProperties properties) {
        return new ApiClient(sapRestClient).setBasePath(properties.baseUrl());
    }

    @Bean
    public HausanschlussApi sapHausanschlussApi(ApiClient sapNetzeApiClient) {
        return new HausanschlussApi(sapNetzeApiClient);
    }

    @Bean
    public AbschlagApi sapNetzeAbschlagApi(ApiClient sapNetzeApiClient) {
        return new AbschlagApi(sapNetzeApiClient);
    }
}
