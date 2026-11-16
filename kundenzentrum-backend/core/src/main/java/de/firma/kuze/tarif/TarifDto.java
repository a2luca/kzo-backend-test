package de.firma.kuze.tarif;

import java.math.BigDecimal;

public record TarifDto(String id, String name, BigDecimal grundpreisEuroProMonat) {
}
