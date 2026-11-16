package de.firma.kuze.sap.odr.tarif;

import de.firma.kuze.sap.odr.CentUmrechnung;
import de.firma.kuze.sap.odr.client.model.SapTarif;
import de.firma.kuze.tarif.TarifDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = CentUmrechnung.class)
public interface TarifMapper {

    @Mapping(target = "id", source = "tarifId")
    @Mapping(target = "name", source = "tarifBez")
    @Mapping(target = "grundpreisEuroProMonat", source = "grundpreisCent", qualifiedByName = "centInEuro")
    TarifDto zuDto(SapTarif tarif);

    List<TarifDto> zuDtos(List<SapTarif> tarife);
}
