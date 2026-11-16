package de.firma.kuze.sap.common.laender;

import de.firma.kuze.laender.LandDto;
import de.firma.kuze.sap.common.client.model.SapLand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LaenderMapper {

    @Mapping(target = "code", source = "landKz")
    @Mapping(target = "name", source = "bezeichnung")
    LandDto zuDto(SapLand land);

    List<LandDto> zuDtos(List<SapLand> laender);
}
