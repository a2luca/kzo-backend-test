package de.firma.kuze.fehler;

/** SAP ist nicht erreichbar oder hat mit einem Serverfehler geantwortet. Wird zu HTTP 502. */
public class SapNichtVerfuegbarException extends RuntimeException {

    public SapNichtVerfuegbarException(String meldung, Throwable ursache) {
        super(meldung, ursache);
    }
}
