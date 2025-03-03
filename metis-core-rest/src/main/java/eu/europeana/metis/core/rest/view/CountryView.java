package eu.europeana.metis.core.rest.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.utils.Country;

/**
 * Represents a view of a country with its enum name, name, and ISO code.
 *
 * @param enumName The enum name of the country.
 * @param name The name of the country.
 * @param isoCode The ISO code of the country.
 */
public record CountryView(
    @JsonProperty("enum") String enumName,
    String name,
    String isoCode
) {

  /**
   * Constructs a {@code CountryView} from a {@code Country} instance.
   *
   * @param country The country instance.
   */
  public CountryView(Country country) {
    this(country.name(), country.getName(), country.getIsoCode());
  }
}
