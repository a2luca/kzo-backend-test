
package de.firma.kuze.laender;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/laender")
public class LaenderController {

    private final LaenderService service;

    public LaenderController(LaenderService service) {
        this.service = service;
    }

    @GetMapping
    public List<LandDto> alleLaender() {
        return service.alleLaender();
    }
}
