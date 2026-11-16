package de.firma.kuze.spectools;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Eine SAP-Spec. Für den Vergleich wird sie mit vollständig aufgelösten {@code $ref} gelesen,
 * für die Teil-Specs als unveränderte Kopie, damit die {@code $ref} auf {@code components} erhalten bleiben.
 */
public record Spec(String name, String inhalt) {

    public static Spec laden(String name, Path datei) {
        try {
            return new Spec(name, Files.readString(datei));
        } catch (IOException e) {
            throw new UncheckedIOException("Spec " + datei + " kann nicht gelesen werden", e);
        }
    }

    public OpenAPI aufgeloest() {
        var optionen = new ParseOptions();
        optionen.setResolve(true);
        optionen.setResolveFully(true);
        return parsen(optionen);
    }

    /** Liefert bei jedem Aufruf eine neue Kopie, die verändert werden darf. */
    public OpenAPI roh() {
        var optionen = new ParseOptions();
        optionen.setResolve(false);
        return parsen(optionen);
    }

    private OpenAPI parsen(ParseOptions optionen) {
        SwaggerParseResult ergebnis = new OpenAPIParser().readContents(inhalt, null, optionen);
        if (ergebnis.getOpenAPI() == null) {
            throw new IllegalArgumentException("Spec " + name + " ist ungültig: " + ergebnis.getMessages());
        }
        return ergebnis.getOpenAPI();
    }
}
