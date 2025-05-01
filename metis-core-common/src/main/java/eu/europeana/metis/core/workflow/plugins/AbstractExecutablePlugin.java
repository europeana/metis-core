package eu.europeana.metis.core.workflow.plugins;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the base implementation of {@link ExecutablePlugin} and all executable plugins should inherit from it.
 *
 * @param <M> The type of the plugin metadata that this plugin represents.
 */
public abstract class AbstractExecutablePlugin<M extends AbstractExecutablePluginMetadata>
    extends AbstractMetisPlugin<M> implements ExecutablePlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String externalTaskId;
  private ExecutionProgress executionProgress = new ExecutionProgress();

  /**
   * Required by (de)serialization in db.
   * <p>It is not to be used manually</p>
   */
  AbstractExecutablePlugin() {
    //Required by (de)serialization in db
  }

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

  @Override
  public String getExternalTaskId() {
    return this.externalTaskId;
  }

  /**
   * @param externalTaskId String representation of the external task identifier of the execution
   */
  public void setExternalTaskId(String externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  @Override
  public ExecutionProgress getExecutionProgress() {
    return this.executionProgress;
  }

  /**
   * @param executionProgress {@link ExecutionProgress} of the external execution
   */
  public void setExecutionProgress(ExecutionProgress executionProgress) {
    this.executionProgress = executionProgress;
  }
}
