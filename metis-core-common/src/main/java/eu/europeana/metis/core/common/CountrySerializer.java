package eu.europeana.metis.core.common;

import eu.europeana.metis.utils.Country;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for {@link Country} enum.
 */
public class CountrySerializer extends StdSerializer<Country> {

  /**
   * Default constructor.
   * Initializes the serializer with the {@link Country} class as the target type.
   * Note: Required, do not remove.
   */
  public CountrySerializer() {
    super(Country.class);
  }

  @Override
  public void serialize(Country country, JsonGenerator generator, SerializationContext provider) throws JacksonException {
    generator.writeStartObject();
    generator.writeName("enum");
    generator.writeString(country.name());
    generator.writeName("name");
    generator.writeString(country.getName());
    generator.writeName("isoCode");
    generator.writeString(country.getIsoCode());
    generator.writeEndObject();
  }
}
