package igrus.web.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.user.domain.Interest;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class InterestListConverter implements AttributeConverter<List<Interest>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Interest> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            List<String> names = attribute.stream().map(Interest::name).toList();
            return objectMapper.writeValueAsString(names);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert interest list to JSON", e);
        }
    }

    @Override
    public List<Interest> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            String json = dbData.trim();
            // H2 JSON 컬럼은 값을 JSON 문자열로 이중 인코딩할 수 있음 (e.g., "\"[...]\"")
            if (json.startsWith("\"")) {
                json = objectMapper.readValue(json, String.class);
            }
            List<String> names = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return new ArrayList<>(names.stream().map(Interest::valueOf).toList());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert JSON to interest list", e);
        }
    }
}
