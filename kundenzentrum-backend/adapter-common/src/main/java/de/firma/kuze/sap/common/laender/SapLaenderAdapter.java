package de.firma.kuze.sap.common.laender;

import de.firma.kuze.laender.LaenderPort;
import de.firma.kuze.laender.LandDto;
import de.firma.kuze.sap.common.client.api.LaenderApi;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SapLaenderAdapter implements LaenderPort {

    private final LaenderApi api;
    private final LaenderMapper mapper;

    public SapLaenderAdapter(LaenderApi api, LaenderMapper mapper) {
        this.api = api;
        this.mapper = mapper;
    }

    @Override
    public List<LandDto> alleLaender() {
        return mapper.zuDtos(api.getLaender());
    }
}
