package de.firma.kuze.mandant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MandantPruefungTest {

    private final MandantPruefung pruefung = new MandantPruefung();

    @Test
    void akzeptiertNetzeUndOdr() {
        assertThat(MandantPruefung.pruefen("netze")).isEqualTo(Mandant.NETZE);
        assertThat(MandantPruefung.pruefen("odr")).isEqualTo(Mandant.ODR);
    }

    @Test
    void ohneMandantBrichtMitVerstaendlicherMeldungAb() {
        assertThatThrownBy(() -> pruefung.postProcessEnvironment(new MockEnvironment(), new SpringApplication()))
                .isInstanceOf(MandantFehltException.class)
                .hasMessage("Kein gültiger Mandant gesetzt (kundenzentrum.mandant=null)");
    }

    @Test
    void unbekannterMandantBrichtAb() {
        var env = new MockEnvironment().withProperty(Mandant.PROPERTY, "NETZE");

        assertThatThrownBy(() -> pruefung.postProcessEnvironment(env, new SpringApplication()))
                .hasMessageContaining("kundenzentrum.mandant=NETZE");
    }
}
