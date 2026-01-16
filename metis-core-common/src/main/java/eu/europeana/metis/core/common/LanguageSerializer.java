package eu.europeana.metis.core.common;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for {@link Language} enum.
 */
public class LanguageSerializer extends StdSerializer<Language> {

  /**
   * Default constructor.
   * Initializes the serializer with the {@link Language} class as the target type.
   * Note: Required, do not remove.
   */
  public LanguageSerializer() {
    super(Language.class);
  }

  @Override
  public void serialize(Language language, JsonGenerator generator, SerializationContext provider) throws JacksonException {
    generator.writeStartObject();
    generator.writeName("enum");
    generator.writeString(language.name());
    generator.writeName("name");
    generator.writeString(language.getName());
    generator.writeEndObject();
  }
}
