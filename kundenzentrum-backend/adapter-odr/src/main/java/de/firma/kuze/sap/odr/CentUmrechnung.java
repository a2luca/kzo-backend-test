package de.firma.kuze.sap.odr;

import org.mapstruct.Named;

import java.math.BigDecimal;

/** Das ODR-SAP liefert Beträge in Cent, das Frontend erwartet Euro. Wird von den ODR-Mappern über {@code uses} eingebunden. */
public final class CentUmrechnung {

    private CentUmrechnung() {
    }

    @Named("centInEuro")
    public static BigDecimal centInEuro(Long cent) {
        return cent == null ? null : BigDecimal.valueOf(cent, 2);
    }
}
