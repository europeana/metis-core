package eu.europeana.metis.core.rest.utils;

import lombok.Setter;
import tools.jackson.databind.ObjectMapper;

/**
 * Utility class with helpful methods for tests
 */
public final class TestUtils {

    @Setter
    private static ObjectMapper objectMapper;

    private TestUtils() {
    }

    public static byte[] convertObjectToJsonBytes(Object object) {
        return objectMapper.writeValueAsBytes(object);
    }
}
