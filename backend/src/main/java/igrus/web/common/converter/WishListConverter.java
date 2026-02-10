package igrus.web.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.user.domain.Wish;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class WishListConverter implements AttributeConverter<List<Wish>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Wish> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            List<String> names = attribute.stream().map(Wish::name).toList();
            return objectMapper.writeValueAsString(names);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert wish list to JSON", e);
        }
    }

    @Override
    public List<Wish> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> names = objectMapper.readValue(dbData, new TypeReference<>() {});
            return new ArrayList<>(names.stream().map(Wish::valueOf).toList());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert JSON to wish list", e);
        }
    }
}
