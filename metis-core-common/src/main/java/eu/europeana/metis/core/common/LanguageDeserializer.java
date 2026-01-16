package eu.europeana.metis.core.common;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializer for {@link Language} enum.
 */
public class LanguageDeserializer extends StdDeserializer<Language> {

  /**
   * Default constructor.
   * Initializes the deserializer with the {@link Language} class as the target type.
   * Note: Required, do not remove.
   */
  public LanguageDeserializer() {
    this(Language.class);
  }

  /**
   * Required as part of {@link StdDeserializer}
   * @param vc required parameter
   */
  public LanguageDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Language deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    JsonNode node = deserializationContext.readTree(jsonParser);
    return Language.getLanguageFromEnumName(node.get("enum").asString());
  }

}
