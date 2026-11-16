package de.firma.kuze.abschlag;

/**
 * Gibt es in beiden Mandanten, aber mit unterschiedlicher SAP-Struktur. Je eine Implementierung
 * in adapter-netze und adapter-odr, jeweils mit @NurNetze bzw. @NurOdr markiert.
 */
public interface AbschlagPort {

    AbschlagDto abschlag(String vertragskontoId);
}
