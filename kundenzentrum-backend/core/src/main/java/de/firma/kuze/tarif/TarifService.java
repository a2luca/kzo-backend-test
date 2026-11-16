package de.firma.kuze.tarif;

import de.firma.kuze.mandant.NurOdr;
import org.springframework.stereotype.Service;

import java.util.List;

@NurOdr
@Service
public class TarifService {

    private final TarifPort port;

    public TarifService(TarifPort port) {
        this.port = port;
    }

    public List<TarifDto> alleTarife() {
        return port.alleTarife();
    }
}
