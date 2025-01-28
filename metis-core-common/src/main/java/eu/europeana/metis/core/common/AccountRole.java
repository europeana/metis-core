package eu.europeana.metis.core.common;

/**
 * Enum containing all possible roles used during authorization.
 * <ul>
 *   <li>{@link AccountRole#ADMIN}: A role that grants full administrative access to the system, including all operations allowed for a {@link AccountRole#DATA_OFFICER} and additional permissions for managing system configurations and resources.</li>
 *   <li>{@link AccountRole#DATA_OFFICER}: A role that allows access to manage datasets, workflows, mappings, and perform other core operations necessary for general system usage.</li>
 * </ul>
 */
public enum AccountRole {
  ADMIN("admin"),
  DATA_OFFICER("data-officer");

  private final String stringRepresentation;

  AccountRole(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
  }

  @Override
  public String toString() {
    return stringRepresentation;
  }
}
