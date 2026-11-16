package de.firma.kuze.hausanschluss;

import de.firma.kuze.mandant.NurNetze;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gibt es nur in der Netze-Instanz. In ODR ist der Pfad unbekannt und liefert 404 wie jede andere unbekannte URL. */
@NurNetze
@RestController
@RequestMapping("/api/hausanschluesse")
public class HausanschlussController {

    private final HausanschlussService service;

    public HausanschlussController(HausanschlussService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public HausanschlussDto hausanschluss(@PathVariable String id) {
        return service.hausanschluss(id);
    }
}
