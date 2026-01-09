package eu.europeana.metis.core.common;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import eu.europeana.metis.utils.Country;

/**
 * Deserializer for {@link Country} enum.
 */
public class CountryDeserializer extends StdDeserializer<Country> {

  private static final long serialVersionUID = 1L;

  /**
   * Default constructor.
   * <p>
   * Initializes the deserializer with the {@link Country} class as the target type.
   * <p>
   * Note: Required, do not remove.
   */
  public CountryDeserializer() {
    this(Country.class);
  }

  /**
   * Required as part of {@link StdDeserializer}
   *
   * @param vc required parameter
   */
  public CountryDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Country deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    JsonNode node = deserializationContext.readTree(jsonParser);
    return Country.getCountryFromEnumName(node.get("enum").asText());
  }
}
