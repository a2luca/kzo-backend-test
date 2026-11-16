package de.firma.kuze.spectools;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpecVergleichTest {

    @Nested
    class BeispielSpecs {

        private final Path verzeichnis = Path.of(System.getProperty("spec.verzeichnis"));
        private final Spec netze = Spec.laden("netze.yaml", verzeichnis.resolve("netze.yaml"));
        private final Spec odr = Spec.laden("odr.yaml", verzeichnis.resolve("odr.yaml"));
        private final VergleichsErgebnis ergebnis = SpecVergleich.vergleiche(netze, odr);

        @Test
        void teiltWieInAbschnitt9Erwartet() {
            assertThat(ergebnis.idsGemeinsam()).containsExactly("getLaender");
            assertThat(ergebnis.idsNetzeRest()).containsExactlyInAnyOrder("getHausanschluss", "getAbschlag");
            assertThat(ergebnis.idsOdrRest()).containsExactlyInAnyOrder("getTarife", "getAbschlag");
        }

        @Test
        void ordnetEinseitigeUndAbweichendeOperationenRichtigZu() {
            assertThat(ergebnis.nurNetze()).extracting(SpecOperation::operationId).containsExactly("getHausanschluss");
            assertThat(ergebnis.nurOdr()).extracting(SpecOperation::operationId).containsExactly("getTarife");
            assertThat(ergebnis.abweichend()).singleElement().satisfies(a -> {
                assertThat(a.operationId()).isEqualTo("getAbschlag");
                assertThat(a.unterschiede()).anySatisfy(u -> assertThat(u)
                        .contains("properties").contains("`betrag`").contains("`betragCent`"));
            });
        }

        @Test
        void teilSpecsEnthaltenNurIhreOperationenUndAlleComponents() {
            OpenAPI common = TeilSpec.bauen(netze, ergebnis.idsGemeinsam(), "gemeinsam");
            OpenAPI netzeRest = TeilSpec.bauen(netze, ergebnis.idsNetzeRest(), "Netze");
            OpenAPI odrRest = TeilSpec.bauen(odr, ergebnis.idsOdrRest(), "ODR");

            assertThat(common.getPaths()).containsOnlyKeys("/laender");
            assertThat(netzeRest.getPaths()).containsOnlyKeys("/hausanschluesse/{anschlussNr}", "/abschlaege/{vertragskonto}");
            assertThat(odrRest.getPaths()).containsOnlyKeys("/tarife", "/abschlaege/{vertragskonto}");
            assertThat(netzeRest.getComponents().getSchemas()).containsOnlyKeys("Land", "Hausanschluss", "Abschlag");

            String yaml = TeilSpec.alsYaml(common);
            assertThat(yaml).contains("$ref: \"#/components/schemas/Land\"").doesNotContain("getHausanschluss");
        }
    }

    @Nested
    class Regeln {

        @Test
        void nurBeschreibungstexteUnterschiedlichGiltAlsIdentisch() {
            var netze = spec("""
                    summary: Länder
                    description: Text A
                    responses:
                      '200':
                        description: OK Netze
                        content:
                          application/json:
                            schema: {$ref: '#/components/schemas/Land'}
                            example: {code: DE}
                    """, """
                    Land:
                      type: object
                      description: Land Netze
                      properties:
                        code: {type: string, description: Code, example: DE}
                    """);
            var odr = spec("""
                    summary: Alle Länder
                    description: Ganz anderer Text B
                    responses:
                      '200':
                        description: OK ODR
                        content:
                          application/json:
                            schema: {$ref: '#/components/schemas/Land'}
                            examples:
                              eins: {value: {code: AT}}
                    """, """
                    Land:
                      type: object
                      description: Land ODR
                      properties:
                        code: {type: string, description: Ländercode, example: AT}
                    """);

            var ergebnis = SpecVergleich.vergleiche(netze, odr);

            assertThat(ergebnis.idsGemeinsam()).containsExactly("getX");
            assertThat(ergebnis.abweichend()).isEmpty();
        }

        @Test
        void schemaFeldMitNameDescriptionGehtNichtVerloren() {
            var mitFeld = spec(antwortMit("""
                    id: {type: string}
                    description: {type: string}
                    """), "");
            var ohneFeld = spec(antwortMit("""
                    id: {type: string}
                    """), "");

            var ergebnis = SpecVergleich.vergleiche(mitFeld, ohneFeld);

            assertThat(ergebnis.abweichend()).singleElement().satisfies(a ->
                    assertThat(a.unterschiede()).containsExactly(
                            "`responses › 200 › content › application/json › schema › properties`: nur in Netze `description`"));
        }

        @Test
        void schemaFeldMitNameDescriptionWirdTrotzdemVerglichen() {
            var text = spec(antwortMit("description: {type: string}\n"), "");
            var zahl = spec(antwortMit("description: {type: integer}\n"), "");

            var ergebnis = SpecVergleich.vergleiche(text, zahl);

            assertThat(ergebnis.abweichend()).singleElement().satisfies(a ->
                    assertThat(a.unterschiede()).singleElement().asString().contains("properties › description › type"));
        }

        @Test
        void unterschiedlicherParameterIstAbweichend() {
            var a = spec("""
                    parameters:
                      - {name: id, in: query, required: true, schema: {type: string}}
                    responses: {'200': {description: OK}}
                    """, "");
            var b = spec("""
                    parameters:
                      - {name: id, in: query, required: false, schema: {type: string}}
                    responses: {'200': {description: OK}}
                    """, "");

            assertThat(SpecVergleich.vergleiche(a, b).idsGemeinsam()).isEmpty();
            assertThat(SpecVergleich.vergleiche(a, b).abweichend()).hasSize(1);
        }

        @Test
        void gleicheOperationIdAnderemPfadIstAbweichend() {
            var a = new Spec("a", kopf() + "  /x:\n    get:\n      operationId: getX\n      responses: {'200': {description: OK}}\n");
            var b = new Spec("b", kopf() + "  /y:\n    get:\n      operationId: getX\n      responses: {'200': {description: OK}}\n");

            var ergebnis = SpecVergleich.vergleiche(a, b);

            assertThat(ergebnis.abweichend()).singleElement()
                    .satisfies(x -> assertThat(x.unterschiede()).containsExactly("`pfad`: Netze `\"/x\"`, ODR `\"/y\"`"));
        }

        @Test
        void operationNurAufEinerSeite() {
            var a = new Spec("a", kopf() + "  /x:\n    get:\n      operationId: getX\n      responses: {'200': {description: OK}}\n");
            var b = new Spec("b", kopf() + "  /y:\n    get:\n      operationId: getY\n      responses: {'200': {description: OK}}\n");

            var ergebnis = SpecVergleich.vergleiche(a, b);

            assertThat(ergebnis.idsGemeinsam()).isEmpty();
            assertThat(ergebnis.idsNetzeRest()).isEqualTo(Set.of("getX"));
            assertThat(ergebnis.idsOdrRest()).isEqualTo(Set.of("getY"));
        }

        @Test
        void operationOhneOperationIdIstEinFehler() {
            var a = new Spec("a", kopf() + "  /x:\n    get:\n      responses: {'200': {description: OK}}\n");

            assertThatThrownBy(() -> SpecVergleich.vergleiche(a, a))
                    .hasMessageContaining("GET /x").hasMessageContaining("keine operationId");
        }

        private static String antwortMit(String properties) {
            return """
                    responses:
                      '200':
                        description: OK
                        content:
                          application/json:
                            schema:
                              type: object
                              properties:
                    """ + properties.indent(20);
        }

        /** Spec mit genau einer Operation {@code GET /x} (operationId {@code getX}). */
        private static Spec spec(String operation, String schemas) {
            String yaml = kopf() + "  /x:\n    get:\n      operationId: getX\n" + operation.indent(6);
            if (!schemas.isBlank()) {
                yaml += "components:\n  schemas:\n" + schemas.indent(4);
            }
            return new Spec("test", yaml);
        }

        private static String kopf() {
            return "openapi: 3.0.3\ninfo: {title: Test, version: '1'}\npaths:\n";
        }
    }
}
