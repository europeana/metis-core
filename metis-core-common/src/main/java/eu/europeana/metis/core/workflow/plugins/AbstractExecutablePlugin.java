package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This abstract class is the base implementation of {@link ExecutablePlugin} and all executable plugins should inherit from it.
 *
 * @param <M> The type of the plugin metadata that this plugin represents.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractExecutablePlugin<M extends AbstractExecutablePluginMetadata>
    extends AbstractMetisPlugin<M> implements ExecutablePlugin {

  private String externalTaskId;
  private String batchId;
  private ExecutionProgress executionProgress = new ExecutionProgress();

  /**
   * Constructor with provided pluginType
   *
   * @param pluginType {@link PluginType}
   */
  AbstractExecutablePlugin(PluginType pluginType) {
    super(pluginType);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata required and the pluginType.
   *
   * @param pluginType a {@link PluginType} related to the implemented plugin
   * @param pluginMetadata the plugin metadata
   */
  AbstractExecutablePlugin(PluginType pluginType, M pluginMetadata) {
    super(pluginType, pluginMetadata);
  }
}
