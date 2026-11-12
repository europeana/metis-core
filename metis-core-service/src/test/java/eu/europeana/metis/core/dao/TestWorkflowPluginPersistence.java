package eu.europeana.metis.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.updates.UpdateOperator;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import java.util.List;
import java.util.stream.Stream;
import org.bson.types.ObjectId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TestWorkflowPluginPersistence {

  @ParameterizedTest
  @MethodSource("updateResults")
  @SuppressWarnings("unchecked")
  void requiresAcknowledgedMatchingUpdate(UpdateResult updateResult, boolean expectedPersisted) {
    MorphiaDatastoreProvider morphiaDatastoreProvider = mock(MorphiaDatastoreProvider.class);
    Datastore datastore = mock(Datastore.class);
    Query<WorkflowExecution> workflowExecutionQuery = mock(Query.class);
    when(morphiaDatastoreProvider.getDatastore()).thenReturn(datastore);
    when(morphiaDatastoreProvider.getInstanceId()).thenReturn("instanceId");
    when(datastore.find(WorkflowExecution.class)).thenReturn(workflowExecutionQuery);
    when(workflowExecutionQuery.filter(any(Filter[].class))).thenReturn(workflowExecutionQuery);
    when(workflowExecutionQuery.update(any(UpdateOptions.class), any(UpdateOperator.class))).thenReturn(updateResult);
    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setMetisPlugins(List.of());
    WorkflowExecutionDao workflowExecutionDao = new WorkflowExecutionDao(morphiaDatastoreProvider);

    assertEquals(expectedPersisted, workflowExecutionDao.updateWorkflowPluginsIfOwned(workflowExecution));
  }

  private static Stream<Arguments> updateResults() {
    return Stream.of(
        Arguments.of(UpdateResult.acknowledged(1, 1L, null), true),
        Arguments.of(UpdateResult.acknowledged(1, 0L, null), true),
        Arguments.of(UpdateResult.acknowledged(0, 0L, null), false),
        Arguments.of(UpdateResult.unacknowledged(), false),
        Arguments.of(null, false));
  }
}
