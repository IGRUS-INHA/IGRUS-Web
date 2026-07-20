package igrus.web.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.user.domain.ProfileLink;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class ProfileLinkListConverter implements AttributeConverter<List<ProfileLink>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ProfileLink> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert profile link list to JSON", e);
        }
    }

    @Override
    public List<ProfileLink> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            String json = dbData.trim();
            // H2 JSON 컬럼은 값을 JSON 문자열로 이중 인코딩할 수 있음 (e.g., "\"[...]\"")
            if (json.startsWith("\"")) {
                json = objectMapper.readValue(json, String.class);
            }
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<ProfileLink>>() {}));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert JSON to profile link list", e);
        }
    }
}
