package de.firma.kuze.spectools;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import java.io.UncheckedIOException;
import java.util.Set;

/**
 * Baut eine Teil-Spec: nur die gewünschten Operationen, {@code components} unverändert.
 * Das ist die einfachste erlaubte Lösung. Folge: Der Generator erzeugt auch Modellklassen,
 * die in der Teil-Spec von keiner Operation benutzt werden.
 */
public final class TeilSpec {

    private TeilSpec() {
    }

    public static OpenAPI bauen(Spec quelle, Set<String> operationIds, String titelZusatz) {
        OpenAPI api = quelle.roh();
        var gefiltert = new Paths();
        if (api.getPaths() != null) {
            api.getPaths().forEach((pfad, pathItem) -> {
                for (PathItem.HttpMethod methode : PathItem.HttpMethod.values()) {
                    Operation op = pathItem.readOperationsMap().get(methode);
                    if (op != null && !operationIds.contains(op.getOperationId())) {
                        pathItem.operation(methode, null);
                    }
                }
                if (!pathItem.readOperations().isEmpty()) {
                    gefiltert.addPathItem(pfad, pathItem);
                }
            });
        }
        api.setPaths(gefiltert);
        api.getInfo().setTitle(api.getInfo().getTitle() + " – " + titelZusatz);
        return api;
    }

    public static String alsYaml(OpenAPI api) {
        try {
            return Yaml.pretty().writeValueAsString(api);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
