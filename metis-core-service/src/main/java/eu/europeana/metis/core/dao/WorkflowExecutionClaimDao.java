package eu.europeana.metis.core.dao;

import static com.mongodb.client.model.Sorts.ascending;
import static eu.europeana.metis.core.common.DaoFieldNames.CLAIMED_BY_INSTANCE;
import static eu.europeana.metis.core.common.DaoFieldNames.CREATED_DATE;
import static eu.europeana.metis.core.common.DaoFieldNames.NEXT_EXECUTABLE_PLUGIN_TYPE;
import static eu.europeana.metis.core.common.DaoFieldNames.UPDATED_DATE;
import static eu.europeana.metis.core.common.DaoFieldNames.WORKFLOW_STATUS;
import static eu.europeana.metis.network.ExternalRequestUtil.retryableExternalRequestForNetworkExceptions;

import com.mongodb.client.model.ReturnDocument;
import dev.morphia.ModifyOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * A Data Access Object (DAO) class for managing the claim and re-queueing operations of {@code WorkflowExecution} entities.
 */
@Repository
public class WorkflowExecutionClaimDao {

  private final MorphiaDatastoreProvider morphiaDatastoreProvider;

  /**
   * Constructor.
   *
   * @param morphiaDatastoreProvider an implementation of {@code MorphiaDatastoreProvider} that provides access to the Morphia
   * {@code Datastore} for MongoDB interactions.
   */
  @Autowired
  public WorkflowExecutionClaimDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    this.morphiaDatastoreProvider = morphiaDatastoreProvider;
  }

  /**
   * Attempts to claim the next available {@code WorkflowExecution} for processing. The method checks for eligible executions in a
   * prioritized order: new in-queue executions, re-queued executions, and stale running executions. If an eligible execution is
   * found, it is claimed and returned. If no eligible execution is found, {@code null} is returned.
   *
   * @param staleLeniency the duration used to determine the staleness threshold for running executions. Executions that have been
   * in a running state without updates for a duration longer than this value are considered stale and eligible for claiming.
   * @param executablePluginType the type of the executable plugin that the claimed execution should be able to process.
   * @return the claimed {@code WorkflowExecution} instance if one is available; {@code null} if no execution could be claimed.
   */
  public WorkflowExecution claimNextExecution(Duration staleLeniency, ExecutablePluginType executablePluginType) {
    Instant now = Instant.now();
    Instant staleBefore = now.minus(staleLeniency);

    ModifyOptions modifyOptions = new ModifyOptions()
        .sort(ascending(UPDATED_DATE.getFieldName(), CREATED_DATE.getFieldName()))
        .returnDocument(ReturnDocument.AFTER)
        .upsert(false);

    List<Supplier<WorkflowExecution>> claimSuppliers = List.of(
        () -> tryClaimStaleRunning(now, modifyOptions, staleBefore, executablePluginType),
        () -> tryClaimInqueueOrRunning(now, modifyOptions, executablePluginType));

    return claimSuppliers.stream().map(Supplier::get).filter(Objects::nonNull).findFirst().orElse(null);
  }

  private WorkflowExecution tryClaimStaleRunning(Instant dateNow, ModifyOptions modifyOptions, Instant staleBefore,
      ExecutablePluginType executablePluginType) {
    Filter[] filters = {
        Filters.eq(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.RUNNING),
        Filters.exists(CLAIMED_BY_INSTANCE.getFieldName()),
        Filters.lt(UPDATED_DATE.getFieldName(), staleBefore),
        Filters.eq(NEXT_EXECUTABLE_PLUGIN_TYPE.getFieldName(), executablePluginType)
    };
    UpdateOperator[] updateOperators = {
        UpdateOperators.set(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.RUNNING),
        UpdateOperators.set(CLAIMED_BY_INSTANCE.getFieldName(), morphiaDatastoreProvider.getInstanceId()),
        UpdateOperators.set(UPDATED_DATE.getFieldName(), dateNow)
    };
    return tryClaim(filters, modifyOptions, updateOperators);
  }

  private WorkflowExecution tryClaimInqueueOrRunning(Instant dateNow, ModifyOptions modifyOptions,
      ExecutablePluginType executablePluginType) {
    Filter[] filters = {
        Filters.in(WORKFLOW_STATUS.getFieldName(), List.of(WorkflowStatus.INQUEUE, WorkflowStatus.RUNNING)),
        Filters.eq(CLAIMED_BY_INSTANCE.getFieldName(), null),
        Filters.eq(NEXT_EXECUTABLE_PLUGIN_TYPE.getFieldName(), executablePluginType)
    };
    UpdateOperator[] updateOperators = {
        UpdateOperators.set(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.RUNNING),
        UpdateOperators.set(CLAIMED_BY_INSTANCE.getFieldName(), morphiaDatastoreProvider.getInstanceId()),
        UpdateOperators.set(UPDATED_DATE.getFieldName(), dateNow)
    };
    return tryClaim(filters, modifyOptions, updateOperators);
  }

  private WorkflowExecution tryClaim(Filter[] filters, ModifyOptions modifyOptions, UpdateOperator[] updateOperators) {
    Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore().find(WorkflowExecution.class)
                                                             .filter(filters);
    return retryableExternalRequestForNetworkExceptions(() -> query.modify(modifyOptions, updateOperators));
  }
}
