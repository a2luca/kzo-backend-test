package de.firma.kuze.abschlag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/abschlag")
public class AbschlagController {

    private final AbschlagService service;

    public AbschlagController(AbschlagService service) {
        this.service = service;
    }

    @GetMapping("/{vertragskontoId}")
    public AbschlagDto abschlag(@PathVariable String vertragskontoId) {
        return service.abschlag(vertragskontoId);
    }
}
