package de.firma.kuze.mandant;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Macht aus {@link MandantFehltException} die übliche "APPLICATION FAILED TO START"-Meldung ohne Stacktrace. */
public class MandantFehltAnalyse extends AbstractFailureAnalyzer<MandantFehltException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MandantFehltException cause) {
        return new FailureAnalysis(
                cause.getMessage() + ". Das Kundenzentrum braucht beim Start genau einen Mandanten.",
                "Starte mit einem der Profile \"netze\" oder \"odr\", z. B. --spring.profiles.active=netze "
                        + "oder SPRING_PROFILES_ACTIVE=odr.",
                cause);
    }
}
