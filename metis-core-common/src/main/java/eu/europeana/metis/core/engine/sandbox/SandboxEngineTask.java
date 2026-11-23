package eu.europeana.metis.core.engine.sandbox;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;

/**
 * Wraps a Sandbox task and exposes its task and output batch identifiers.
 *
 * @param sandboxTask the Sandbox task
 */
public record SandboxEngineTask(SandboxTask sandboxTask) implements EngineTask {

  @Override
  public String getEngineTaskId() {
    return sandboxTask.getTaskId();
  }

  @Override
  public String getEngineBatchId() {
    return sandboxTask.getBatchId();
  }
}
