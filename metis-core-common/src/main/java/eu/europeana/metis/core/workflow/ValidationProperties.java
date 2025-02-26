package eu.europeana.metis.core.workflow;

/**
 * Represents extra properties that are needed for validation.
 */
public record ValidationProperties(String urlOfSchemasZip, String schemaRootPath, String schematronRootPath) {}
