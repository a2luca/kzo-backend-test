package de.firma.kuze.mandant;

import java.util.Arrays;
import java.util.Optional;

/** Die Mandanten. Der Wert in {@code kundenzentrum.mandant} ist {@link #propertyWert()}. */
public enum Mandant {
    NETZE("netze"),
    ODR("odr");

    public static final String PROPERTY = "kundenzentrum.mandant";

    private final String propertyWert;

    Mandant(String propertyWert) {
        this.propertyWert = propertyWert;
    }

    public String propertyWert() {
        return propertyWert;
    }

    public static Optional<Mandant> ausPropertyWert(String wert) {
        return Arrays.stream(values()).filter(m -> m.propertyWert.equals(wert)).findFirst();
    }
}
