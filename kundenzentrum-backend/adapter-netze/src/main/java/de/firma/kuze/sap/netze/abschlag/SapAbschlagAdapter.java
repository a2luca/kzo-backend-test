package de.firma.kuze.sap.netze.abschlag;

import de.firma.kuze.abschlag.AbschlagDto;
import de.firma.kuze.abschlag.AbschlagPort;
import de.firma.kuze.mandant.NurNetze;
import de.firma.kuze.sap.netze.client.api.AbschlagApi;
import org.springframework.stereotype.Component;

@NurNetze
@Component
public class SapAbschlagAdapter implements AbschlagPort {

    private final AbschlagApi api;
    private final AbschlagNetzeMapper mapper;

    public SapAbschlagAdapter(AbschlagApi api, AbschlagNetzeMapper mapper) {
        this.api = api;
        this.mapper = mapper;
    }

    @Override
    public AbschlagDto abschlag(String vertragskontoId) {
        return mapper.zuDto(api.getAbschlag(vertragskontoId));
    }
}
