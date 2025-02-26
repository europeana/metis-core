package eu.europeana.metis.core.rest.stats;

/**
 * Represents statistics for a specific attribute.
 *
 * @param xPath The XPath expression of the attribute.
 * @param value The value of the attribute.
 * @param occurrences The number of times this attribute value appears.
 */
public record AttributeStatistics(String xPath, String value, long occurrences) {}
