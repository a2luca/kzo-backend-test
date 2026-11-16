package de.firma.kuze.abschlag;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbschlagDto(BigDecimal betragEuro, LocalDate naechsteFaelligkeit) {
}
