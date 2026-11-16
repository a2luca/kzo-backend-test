package de.firma.kuze.fehler;

/** Der angefragte Datensatz existiert im Backend-System nicht. Wird zu HTTP 404. */
public class NichtGefundenException extends RuntimeException {

    public NichtGefundenException(String meldung, Throwable ursache) {
        super(meldung, ursache);
    }
}
