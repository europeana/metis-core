package eu.europeana.metis.core.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSerializationUtils {

  public static void assertFieldEquals(JsonNode jsonNode, String fieldName, String expectedValue) {
    assertEquals(expectedValue, jsonNode.get(fieldName).asText());
  }

  public static void assertNestedFieldEquals(JsonNode jsonNode, String parentField, String nestedField, String expectedValue) {
    assertEquals(expectedValue, jsonNode.get(parentField).get(nestedField).asText());
  }

  public static void assertListContains(JsonNode jsonNode, String... expectedValues) {
    for (String value : expectedValues) {
      assertTrue(jsonNode.toString().contains(value));
    }
  }

  public static void assertNestedFieldInArrayEquals(JsonNode jsonNode, String parentField, String nestedField, String expectedValue) {
    JsonNode parentArray = jsonNode.get(parentField);
    List<String> nestedValues = new ArrayList<>();
    for (JsonNode arrayElement : parentArray) {
      if (arrayElement.has(nestedField)) {
        nestedValues.add(arrayElement.get(nestedField).asText());
      }
    }
    assertTrue(nestedValues.contains(expectedValue));
  }

  public static String formatAsUTC(Date date) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return simpleDateFormat.format(date);
  }
}
