package de.firma.kuze.tarif;

import de.firma.kuze.mandant.NurOdr;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gibt es nur in der ODR-Instanz. In Netze ist der Pfad unbekannt und liefert 404 wie jede andere unbekannte URL. */
@NurOdr
@RestController
@RequestMapping("/api/tarife")
public class TarifController {

    private final TarifService service;

    public TarifController(TarifService service) {
        this.service = service;
    }

    @GetMapping
    public List<TarifDto> alleTarife() {
        return service.alleTarife();
    }
}
