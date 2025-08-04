package eu.europeana.metis.core.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;

import eu.europeana.metis.core.common.RecordIdUtils.DatasetIdAndRecordId;
import eu.europeana.metis.exception.BadContentException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TestRecordIdUtils {

  @ParameterizedTest
  @MethodSource
  void testDecomposeFullRecordIdValidInput(String input, String expectedDatasetId, String expectedRecordId) {
    DatasetIdAndRecordId result = RecordIdUtils.decomposeFullRecordId(input);
    assertNotNull(result);
    assertEquals(expectedDatasetId, result.datasetId());
    assertEquals(expectedRecordId, result.recordId());
  }

  @ParameterizedTest
  @MethodSource
  void testDecomposeFullRecordIdInvalidInput(String input) {
    assertNull(RecordIdUtils.decomposeFullRecordId(input));
  }

  private static Stream<Arguments> testDecomposeFullRecordIdValidInput() {
    return Stream.of(
        of("/1/A", "1", "A"),
        of("/123/ABC", "123", "ABC")
    );
  }

  private static Stream<Arguments> testDecomposeFullRecordIdInvalidInput() {
    return Stream.of(
        of("//"),
        of("/1/"),
        of("//A"),
        of("//1/A"),
        of("/1//A"),
        of("1/A"),
        of("1A"),
        of(" /1/A"),
        of("/1/A "),
        of("/ 1/A"),
        of("/1 /A"),
        of("/1/ A")
    );
  }

  @ParameterizedTest
  @MethodSource
  void testNormalizeRecordIdWithValidInput(String datasetId, String recordId, String expectedNormalizedRecordId)
      throws BadContentException {
    assertEquals(Optional.ofNullable(expectedNormalizedRecordId), RecordIdUtils.checkAndNormalizeRecordId(datasetId, recordId));
  }

  @ParameterizedTest
  @MethodSource
  void testNormalizeRecordIdWithInvalidInput(
      String datasetId, String recordId, Class<? extends Throwable> expectedExceptionType) {
    assertThrows(expectedExceptionType, () -> RecordIdUtils.checkAndNormalizeRecordId(datasetId, recordId));
  }

  private static Stream<Arguments> testNormalizeRecordIdWithValidInput() {
    return Stream.of(
        //Empty recordIds
        of("dataset1", "", null),
        of("dataset1", " ", null),

        //Spaced recordIds
        of("dataset1", "id1", "id1"),
        of("dataset1", " id2", "id2"),
        of("dataset1", "id3 ", "id3"),

        //Prefixed recordIds
        of("dataset1", "/id1", "id1"),
        of("dataset1", "dataset1/id2", "id2"),
        of("dataset1", "/dataset1/id3", "id3"),
        of("dataset1", "a/dataset1/id1", "id1"),
        of("dataset1", "http://a/dataset1/id2", "id2"),
        of("dataset1", "file://a/dataset1/id3", "id3")
    );
  }

  private static Stream<Arguments> testNormalizeRecordIdWithInvalidInput() {
    return Stream.of(
        of("dataset1", "id1/", BadContentException.class),
        of("dataset1", "/dataset1/id1/", BadContentException.class),
        of("dataset1", "/dataset1//id2", BadContentException.class),
        of("dataset1", "dataset2/id3", BadContentException.class),
        of("dataset1", "/dataset2/id1", BadContentException.class),
        of("dataset1", "dataset1/ id1", BadContentException.class),
        of("dataset1", "dataset1 /id2", BadContentException.class),
        of("dataset1", "dataset 1/id3", BadContentException.class),
        of("dataset1", "test 1/dataset1/id1", BadContentException.class),
        of("dataset1", "dataset1/id-2", BadContentException.class),
        of("dataset1", "dataset1/i?d3", BadContentException.class),
        of("dataset1", "(dataset1)/id1", BadContentException.class)
    );
  }
}
