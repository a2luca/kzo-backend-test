package de.firma.kuze.sap.netze.hausanschluss;

import de.firma.kuze.hausanschluss.HausanschlussDto;
import de.firma.kuze.hausanschluss.HausanschlussStatus;
import de.firma.kuze.sap.netze.client.model.SapHausanschluss;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = "spring")
public interface HausanschlussMapper {

    @Mapping(target = "id", source = "anschlussNr")
    @Mapping(target = "adresse", expression = "java(adresse(hausanschluss))")
    @Mapping(target = "status", source = "statusKz")
    HausanschlussDto zuDto(SapHausanschluss hausanschluss);

    @ValueMapping(source = "B", target = "IN_BETRIEB")
    @ValueMapping(source = "P", target = "IN_PLANUNG")
    @ValueMapping(source = "S", target = "STILLGELEGT")
    HausanschlussStatus status(SapHausanschluss.StatusKzEnum statusKz);

    /** "Musterweg 1, 12345 Musterstadt" */
    default String adresse(SapHausanschluss h) {
        return h.getStrasse() + " " + h.getHausNr() + ", " + h.getPlz() + " " + h.getOrt();
    }
}
