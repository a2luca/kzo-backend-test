package de.firma.kuze.hausanschluss;

import de.firma.kuze.mandant.NurNetze;
import org.springframework.stereotype.Service;

@NurNetze
@Service
public class HausanschlussService {

    private final HausanschlussPort port;

    public HausanschlussService(HausanschlussPort port) {
        this.port = port;
    }

    public HausanschlussDto hausanschluss(String id) {
        return port.hausanschluss(id);
    }
}
