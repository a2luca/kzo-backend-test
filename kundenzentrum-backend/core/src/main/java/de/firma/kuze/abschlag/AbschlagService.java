package de.firma.kuze.abschlag;

import org.springframework.stereotype.Service;

@Service
public class AbschlagService {

    private final AbschlagPort port;

    public AbschlagService(AbschlagPort port) {
        this.port = port;
    }

    public AbschlagDto abschlag(String vertragskontoId) {
        return port.abschlag(vertragskontoId);
    }
}
