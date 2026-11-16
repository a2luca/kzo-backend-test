package de.firma.kuze.sap.odr.abschlag;

import de.firma.kuze.abschlag.AbschlagDto;
import de.firma.kuze.sap.odr.CentUmrechnung;
import de.firma.kuze.sap.odr.client.model.SapAbschlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Der Abschlagsrhythmus aus dem ODR-SAP gehört nicht zum Frontend-Vertrag und wird nicht übernommen.
 * Zum Namen siehe {@code AbschlagNetzeMapper}.
 */
@Mapper(componentModel = "spring", uses = CentUmrechnung.class)
public interface AbschlagOdrMapper {

    @Mapping(target = "betragEuro", source = "betragCent", qualifiedByName = "centInEuro")
    @Mapping(target = "naechsteFaelligkeit", source = "faelligkeit")
    AbschlagDto zuDto(SapAbschlag abschlag);
}
