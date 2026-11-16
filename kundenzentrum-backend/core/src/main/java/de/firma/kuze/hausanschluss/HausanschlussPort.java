package de.firma.kuze.hausanschluss;

/** Wird nur in der Netze-Instanz implementiert. Das Interface selbst braucht keine Mandanten-Annotation. */
public interface HausanschlussPort {

    HausanschlussDto hausanschluss(String id);
}
