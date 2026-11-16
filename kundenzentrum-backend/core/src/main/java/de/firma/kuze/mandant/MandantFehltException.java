package de.firma.kuze.mandant;

/** Beim Start ist kein oder ein unbekannter Mandant gesetzt. */
public class MandantFehltException extends IllegalStateException {

    private final String wert;

    public MandantFehltException(String wert) {
        super("Kein gültiger Mandant gesetzt (" + Mandant.PROPERTY + "=" + wert + ")");
        this.wert = wert;
    }

    public String wert() {
        return wert;
    }
}
