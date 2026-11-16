package de.firma.kuze.fehler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FehlerHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FehlerHandler.class);

    @ExceptionHandler(NichtGefundenException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public FehlerDto nichtGefunden(NichtGefundenException e) {
        LOG.debug("Nicht gefunden: {}", e.getMessage());
        return FehlerDto.NICHT_GEFUNDEN;
    }

    @ExceptionHandler(SapNichtVerfuegbarException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public FehlerDto sapNichtVerfuegbar(SapNichtVerfuegbarException e) {
        LOG.warn("SAP nicht verfügbar: {}", e.getMessage(), e);
        return FehlerDto.SAP_NICHT_VERFUEGBAR;
    }
}
