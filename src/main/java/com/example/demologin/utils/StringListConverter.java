package com.example.demologin.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Converter
@Slf4j
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            if (attribute == null || attribute.isEmpty()) {
                return "[]";
            }
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (Exception ex) {
            log.error("Failed to serialize image URL list", ex);
            return "[]";
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return new ArrayList<>();
            }
            return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            log.error("Failed to deserialize image URL list", ex);
            return new ArrayList<>();
        }
    }
}
