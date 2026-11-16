package de.firma.kuze.sap.netze.hausanschluss;

import de.firma.kuze.hausanschluss.HausanschlussDto;
import de.firma.kuze.hausanschluss.HausanschlussPort;
import de.firma.kuze.mandant.NurNetze;
import de.firma.kuze.sap.netze.client.api.HausanschlussApi;
import org.springframework.stereotype.Component;

@NurNetze
@Component
public class SapHausanschlussAdapter implements HausanschlussPort {

    private final HausanschlussApi api;
    private final HausanschlussMapper mapper;

    public SapHausanschlussAdapter(HausanschlussApi api, HausanschlussMapper mapper) {
        this.api = api;
        this.mapper = mapper;
    }

    @Override
    public HausanschlussDto hausanschluss(String id) {
        return mapper.zuDto(api.getHausanschluss(id));
    }
}
