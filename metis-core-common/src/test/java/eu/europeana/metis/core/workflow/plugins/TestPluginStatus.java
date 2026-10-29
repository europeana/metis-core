package eu.europeana.metis.core.workflow.plugins;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TestPluginStatus {

  @ParameterizedTest
  @EnumSource(value = PluginStatus.class, names = {"INQUEUE", "CLEANING", "RUNNING",
      "IDENTIFYING_DELETED_RECORDS", "PENDING"})
  void nonTerminalPluginStatusIsRunnable(PluginStatus pluginStatus) {
    assertTrue(pluginStatus.isRunnable());
  }

  @ParameterizedTest
  @EnumSource(value = PluginStatus.class, names = {"FINISHED", "CANCELLED", "FAILED"})
  void terminalPluginStatusIsNotRunnable(PluginStatus pluginStatus) {
    assertFalse(pluginStatus.isRunnable());
  }

  @ParameterizedTest
  @EnumSource(value = PluginStatus.class, names = {"INQUEUE", "RUNNING", "IDENTIFYING_DELETED_RECORDS"})
  void cancellablePluginStatusCanBeCancelled(PluginStatus pluginStatus) {
    assertTrue(pluginStatus.isCancellable());
  }

  @ParameterizedTest
  @EnumSource(value = PluginStatus.class, names = {"CLEANING", "PENDING", "FINISHED", "CANCELLED", "FAILED"})
  void protectedOrTerminalPluginStatusCannotBeCancelled(PluginStatus pluginStatus) {
    assertFalse(pluginStatus.isCancellable());
  }
}
