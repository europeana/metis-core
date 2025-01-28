package eu.europeana.metis.core.common;

/**
 * Enum containing all possible roles used during authorization.
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
