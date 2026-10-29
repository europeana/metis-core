package eu.europeana.metis.core.engine.sandbox;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;

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
