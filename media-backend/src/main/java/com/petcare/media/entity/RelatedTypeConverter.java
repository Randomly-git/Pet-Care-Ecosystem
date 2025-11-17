// entity/RelatedTypeConverter.java
package com.petcare.media.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RelatedTypeConverter implements AttributeConverter<RelatedType, String> {

    @Override
    public String convertToDatabaseColumn(RelatedType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public RelatedType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return RelatedType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的关联类型: " + dbData);
        }
    }
}