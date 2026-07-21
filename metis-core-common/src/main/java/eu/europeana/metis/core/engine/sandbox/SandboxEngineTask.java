package eu.europeana.metis.core.engine.sandbox;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;

public record SandboxEngineTask(SandboxTask sandboxTask) implements EngineTask {

  @Override
  public String getExternalTaskId() {
    //todo: Not supported yet in sandbox
    return null;
  }

  @Override
  public String getBatchId() {
    //todo: Not supported yet in sandbox
    return null;
  }
}
