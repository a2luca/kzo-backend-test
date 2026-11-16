package de.firma.kuze.sap.odr.tarif;

import de.firma.kuze.mandant.NurOdr;
import de.firma.kuze.sap.odr.client.api.TarifApi;
import de.firma.kuze.tarif.TarifDto;
import de.firma.kuze.tarif.TarifPort;
import org.springframework.stereotype.Component;

import java.util.List;

@NurOdr
@Component
public class SapTarifAdapter implements TarifPort {

    private final TarifApi api;
    private final TarifMapper mapper;

    public SapTarifAdapter(TarifApi api, TarifMapper mapper) {
        this.api = api;
        this.mapper = mapper;
    }

    @Override
    public List<TarifDto> alleTarife() {
        return mapper.zuDtos(api.getTarife());
    }
}
