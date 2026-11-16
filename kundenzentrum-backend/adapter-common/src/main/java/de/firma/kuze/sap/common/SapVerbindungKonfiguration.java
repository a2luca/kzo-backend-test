package de.firma.kuze.sap.common;

import de.firma.kuze.sap.common.client.ApiClient;
import de.firma.kuze.sap.common.client.api.LaenderApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Gemeinsame SAP-Verbindung für beide Mandanten. Hier gibt es keine Mandanten-Annotationen:
 * Derselbe Code läuft in beiden Instanzen, nur die Base-URL unterscheidet sich.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SapProperties.class)
public class SapVerbindungKonfiguration {

    /** Der RestClient, den alle generierten Clients (common, netze, odr) benutzen. */
    @Bean
    public RestClient sapRestClient(RestClient.Builder builder, SapProperties properties) {
        return SapFehlerUebersetzung.anwenden(builder
                        .requestInterceptor(new BasicAuthenticationInterceptor(properties.user(), properties.password())))
                .build();
    }

    @Bean
    public ApiClient sapCommonApiClient(RestClient sapRestClient, SapProperties properties) {
        return new ApiClient(sapRestClient).setBasePath(properties.baseUrl());
    }

    @Bean
    public LaenderApi sapLaenderApi(ApiClient sapCommonApiClient) {
        return new LaenderApi(sapCommonApiClient);
    }
}
