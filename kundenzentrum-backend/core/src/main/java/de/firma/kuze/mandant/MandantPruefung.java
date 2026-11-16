package de.firma.kuze.mandant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Bricht den Start ab, wenn kein gültiger Mandant gesetzt ist. Läuft vor dem Aufbau des Kontexts,
 * damit nicht erst irgendeine Bean mit einer unverständlichen Meldung scheitert.
 * Angemeldet in {@code META-INF/spring.factories}, die Meldung formuliert {@link MandantFehltAnalyse}.
 */
public class MandantPruefung implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        pruefen(environment.getProperty(Mandant.PROPERTY));
    }

    static Mandant pruefen(String wert) {
        return Mandant.ausPropertyWert(wert).orElseThrow(() -> new MandantFehltException(wert));
    }
}
