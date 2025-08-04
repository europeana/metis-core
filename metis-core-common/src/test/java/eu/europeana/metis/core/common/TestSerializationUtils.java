package eu.europeana.metis.core.common;

import com.jayway.jsonpath.JsonPath;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSerializationUtils {

  private static final SimpleDateFormat UTC_DATE_FORMAT;

  static {
    UTC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    UTC_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static <T> void assertFieldEquals(String jsonOutput, String fieldName, T expectedValue) {
    if (expectedValue instanceof Date expectedDate) {
      String formattedDate = formatAsUTC(expectedDate);
      assertThat(JsonPath.read(jsonOutput, format("$.%s", fieldName)), is(formattedDate));
    } else {
      assertThat(JsonPath.read(jsonOutput, format("$.%s", fieldName)), is(expectedValue));
    }
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

  private static String formatAsUTC(Date date) {
    return UTC_DATE_FORMAT.format(date);
  }
}
