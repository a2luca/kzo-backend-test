package de.firma.kuze.sap.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Verbindung zum SAP der jeweiligen Instanz. Gesetzt in application-netze.yaml bzw. application-odr.yaml. */
@ConfigurationProperties("kundenzentrum.sap")
public record SapProperties(String baseUrl, String user, String password) {
}
