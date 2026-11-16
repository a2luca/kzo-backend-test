package de.firma.kuze.spectools;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.Map;
import java.util.TreeMap;

/** Eine Operation mit dem Ort, an dem sie in der Spec steht. */
public record SpecOperation(String operationId, PathItem.HttpMethod methode, String pfad,
                            PathItem pathItem, Operation operation) {

    public String anzeige() {
        return methode + " " + pfad;
    }

    /** Alle Operationen einer Spec, sortiert nach {@code operationId}. */
    public static Map<String, SpecOperation> alle(OpenAPI api, String specName) {
        var ergebnis = new TreeMap<String, SpecOperation>();
        if (api.getPaths() == null) {
            return ergebnis;
        }
        api.getPaths().forEach((pfad, pathItem) -> pathItem.readOperationsMap().forEach((methode, operation) -> {
            String id = operation.getOperationId();
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Spec " + specName + ": Operation " + methode + " " + pfad + " hat keine operationId");
            }
            var vorher = ergebnis.put(id, new SpecOperation(id, methode, pfad, pathItem, operation));
            if (vorher != null) {
                throw new IllegalArgumentException(
                        "Spec " + specName + ": operationId " + id + " ist doppelt vergeben");
            }
        }));
        return ergebnis;
    }
}
