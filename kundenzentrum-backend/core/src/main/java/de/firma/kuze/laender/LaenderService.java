package de.firma.kuze.laender;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LaenderService {

    private final LaenderPort port;

    public LaenderService(LaenderPort port) {
        this.port = port;
    }

    public List<LandDto> alleLaender() {
        return port.alleLaender();
    }
}
