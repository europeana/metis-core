package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;
import java.util.Set;

/**
 * The status that a plugin can have.
 */
public enum PluginStatus {
  INQUEUE(Category.RUNNABLE, Category.CANCELLABLE),
  RUNNING(Category.RUNNABLE, Category.CANCELLABLE),
  IDENTIFYING_DELETED_RECORDS(Category.RUNNABLE, Category.CANCELLABLE),
  CLEANING(Category.RUNNABLE),
  PENDING(Category.RUNNABLE),
  FINISHED(Category.TERMINAL),
  CANCELLED(Category.TERMINAL),
  FAILED(Category.TERMINAL);

  private final Set<Category> categories;

  PluginStatus(Category... categories) {
    this.categories = Set.of(categories);
  }

  public boolean isRunnable() {
    return categories.contains(Category.RUNNABLE);
  }

  public boolean isCancellable() {
    return categories.contains(Category.CANCELLABLE);
  }

  /**
   * Lookup of a {@link PluginStatus} enum from a provided enum String representation of the enum
   * value.
   *
   * @param enumName the String representation of an enum value
   * @return the {@link PluginStatus} that represents the provided value or null if not found
   */
  @JsonCreator
  public static PluginStatus getPluginStatusFromEnumName(String enumName) {
    return PluginStatus.valueOf(enumName.toUpperCase(Locale.ROOT));
  }

  enum Category {
    CANCELLABLE,
    RUNNABLE,
    TERMINAL
  }
}
