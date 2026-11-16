package de.firma.kuze.tarif;

import java.util.List;

/** Wird nur in der ODR-Instanz implementiert. Das Interface selbst braucht keine Mandanten-Annotation. */
public interface TarifPort {

    List<TarifDto> alleTarife();
}
