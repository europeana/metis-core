package eu.europeana.metis.core.common;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;

public class TestSerializationUtils {

  public static <T> void assertFieldEquals(String jsonOutput, String fieldName, T expectedValue) {
    Object expected = expectedValue instanceof Instant instant
        ? instant.toString()
        : expectedValue;

    assertThat(
        JsonPath.read(jsonOutput, format("$.%s", fieldName)),
        is(expected)
    );
  }

  public static <T> void assertNestedFieldEquals(String jsonOutput, String parentField, String nestedField, T expectedValue) {
    assertThat(JsonPath.read(jsonOutput, format("$.%s.%s", parentField, nestedField)), is(expectedValue));
  }

  public static <T> void assertNestedFieldInArrayEquals(String jsonOutput, String parentField, String nestedField,
      T expectedValue) {
    List<T> nestedValues = JsonPath.read(jsonOutput, format("$.%s[*].%s", parentField, nestedField));
    assertTrue(nestedValues.contains(expectedValue));
  }

  public static <T> void assertListContains(String jsonOutput, String listField, List<T> expectedValues) {
    List<T> actualValues = JsonPath.read(jsonOutput, listField);
    for (T value : expectedValues) {
      assertTrue(actualValues.contains(value));
    }
  }
}
