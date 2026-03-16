package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * The status that a plugin can have.
 */
public enum PluginStatus {
  INQUEUE(Category.RUNNABLE),
  CLEANING(Category.RUNNABLE),
  RUNNING(Category.RUNNABLE),
  IDENTIFYING_DELETED_RECORDS(Category.RUNNABLE),
  PENDING(Category.RUNNABLE),

  FINISHED(Category.TERMINAL),
  CANCELLED(Category.TERMINAL),
  FAILED(Category.TERMINAL);

  private final Category category;

  PluginStatus(Category category) {
    this.category = category;
  }

  public boolean isRunnable() {
    return category == Category.RUNNABLE;
  }

  public boolean isCancellable() {
    return this != CLEANING && this != PENDING;
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
    RUNNABLE,
    TERMINAL
  }
}
