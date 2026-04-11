package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DevisStatusConverter implements AttributeConverter<DevisStatut, String> {

    @Override
    public String convertToDatabaseColumn(DevisStatut statut) {
        if (statut == null) return null;
        return statut == DevisStatut.EN_COURS ? "EN_CREATION" : "CLOS";
    }

    @Override
    public DevisStatut convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return DevisStatut.EN_COURS;
        switch (value.toLowerCase()) {
            case "actif":
            case "en_creation":
                return DevisStatut.EN_COURS;
            case "inactif":
            case "clos":
            case "cloture":
                return DevisStatut.CLOS;
            default:
                return DevisStatut.EN_COURS;
        }
    }
}