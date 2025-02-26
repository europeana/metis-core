package eu.europeana.metis.core.rest.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.core.common.Language;

/**
 * Represents a view of a language with its enum name and display name.
 *
 * @param enumName The enum name of the language.
 * @param name The display name of the language.
 */
public record LanguageView(
    @JsonProperty("enum") String enumName,
    String name
) {
  /**
   * Constructs a {@code LanguageView} from a {@code Language} instance.
   *
   * @param language The language instance.
   */
  public LanguageView(Language language) {
    this(language.name(), language.getName());
  }
}
