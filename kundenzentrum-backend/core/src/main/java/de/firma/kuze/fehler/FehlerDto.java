package de.firma.kuze.fehler;

public record FehlerDto(String fehler) {

    public static final FehlerDto NICHT_GEFUNDEN = new FehlerDto("NICHT_GEFUNDEN");
    public static final FehlerDto SAP_NICHT_VERFUEGBAR = new FehlerDto("SAP_NICHT_VERFUEGBAR");
}
