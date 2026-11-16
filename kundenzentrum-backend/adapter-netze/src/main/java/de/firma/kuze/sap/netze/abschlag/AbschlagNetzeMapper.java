package de.firma.kuze.sap.netze.abschlag;

import de.firma.kuze.abschlag.AbschlagDto;
import de.firma.kuze.sap.netze.client.model.SapAbschlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Heißt bewusst nicht nur "AbschlagMapper": MapStruct-Implementierungen tragen keine Mandanten-Annotation,
 * zwei gleichnamige Mapper in adapter-netze und adapter-odr ergäben denselben Bean-Namen "abschlagMapperImpl".
 */
@Mapper(componentModel = "spring")
public interface AbschlagNetzeMapper {

    @Mapping(target = "betragEuro", source = "betrag")
    @Mapping(target = "naechsteFaelligkeit", source = "faelligAm")
    AbschlagDto zuDto(SapAbschlag abschlag);

    /** Netze liefert Euro als Gleitkommazahl. Das Frontend bekommt immer zwei Nachkommastellen. */
    default BigDecimal euro(Double betrag) {
        return betrag == null ? null : BigDecimal.valueOf(betrag).setScale(2, RoundingMode.HALF_UP);
    }
}
