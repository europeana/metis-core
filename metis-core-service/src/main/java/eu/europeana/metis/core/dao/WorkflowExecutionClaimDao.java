package eu.europeana.metis.core.dao;

import static com.mongodb.client.model.Sorts.ascending;
import static eu.europeana.metis.core.common.DaoFieldNames.CLAIMED_BY_INSTANCE;
import static eu.europeana.metis.core.common.DaoFieldNames.CREATED_DATE;
import static eu.europeana.metis.core.common.DaoFieldNames.ID;
import static eu.europeana.metis.core.common.DaoFieldNames.STARTED_DATE;
import static eu.europeana.metis.core.common.DaoFieldNames.UPDATED_DATE;
import static eu.europeana.metis.core.common.DaoFieldNames.WORKFLOW_STATUS;
import static eu.europeana.metis.network.ExternalRequestUtil.retryableExternalRequestForNetworkExceptions;

import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.ModifyOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
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
   * @return the claimed {@code WorkflowExecution} instance if one is available; {@code null} if no execution could be claimed.
   */
  public WorkflowExecution claimNextExecution(Duration staleLeniency) {
    Instant now = Instant.now();
    Date dateNow = Date.from(now);
    Date staleBefore = Date.from(now.minus(staleLeniency));

    ModifyOptions modifyOptions = new ModifyOptions()
        .sort(ascending(CREATED_DATE.getFieldName()))
        .returnDocument(ReturnDocument.AFTER)
        .upsert(false);

    List<Supplier<WorkflowExecution>> claimSuppliers = List.of(
        () -> tryClaimStaleRunning(dateNow, modifyOptions, staleBefore),
        () -> tryClaimRequeuedInqueue(dateNow, modifyOptions),
        () -> tryClaimNewInqueue(dateNow, modifyOptions));

    for (Supplier<WorkflowExecution> claimSupplier : claimSuppliers) {
      WorkflowExecution workflowExecution = claimSupplier.get();
      if (workflowExecution != null) {
        return workflowExecution;
      }
    }
    return null;
  }

  private WorkflowExecution tryClaimNewInqueue(Date dateNow, ModifyOptions modifyOptions) {
    Filter[] filters = {
        Filters.eq(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.INQUEUE),
        Filters.eq(CLAIMED_BY_INSTANCE.getFieldName(), null),
        Filters.eq(STARTED_DATE.getFieldName(), null)
    };
    UpdateOperator[] updateOperators = {
        UpdateOperators.set(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.RUNNING),
        UpdateOperators.set(CLAIMED_BY_INSTANCE.getFieldName(), morphiaDatastoreProvider.getInstanceId()),
        UpdateOperators.set(UPDATED_DATE.getFieldName(), dateNow),
        UpdateOperators.set(STARTED_DATE.getFieldName(), dateNow)
    };
    return tryClaim(filters, modifyOptions, updateOperators);
  }

  private WorkflowExecution tryClaimRequeuedInqueue(Date dateNow, ModifyOptions modifyOptions) {
    Filter[] filters = {
        Filters.eq(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.INQUEUE),
        Filters.eq(CLAIMED_BY_INSTANCE.getFieldName(), null),
        Filters.exists(STARTED_DATE.getFieldName())
    };
    UpdateOperator[] updateOperators = {
        UpdateOperators.set(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.RUNNING),
        UpdateOperators.set(CLAIMED_BY_INSTANCE.getFieldName(), morphiaDatastoreProvider.getInstanceId()),
        UpdateOperators.set(UPDATED_DATE.getFieldName(), dateNow)
    };
    return tryClaim(filters, modifyOptions, updateOperators);
  }

  private WorkflowExecution tryClaimStaleRunning(Date dateNow, ModifyOptions modifyOptions, Date staleBefore) {
    Filter[] filters = {
        Filters.eq(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.RUNNING),
        Filters.exists(CLAIMED_BY_INSTANCE.getFieldName()),
        Filters.lt(UPDATED_DATE.getFieldName(), staleBefore)
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
    return retryableExternalRequestForNetworkExceptions(() ->
        query.modify(modifyOptions, updateOperators)
    );
  }

  /**
   * Re-queues a given workflow execution by changing its status to "INQUEUE" and unsetting the instance that has claimed it if
   * certain conditions are met.
   *
   * @param workflowExecution The workflow execution object to be re-queued. It must contain an ID, claimed instance, and status.
   * @return true if the workflow execution was successfully updated; false otherwise.
   */
  public boolean requeue(WorkflowExecution workflowExecution) {
    Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
                                                             .find(WorkflowExecution.class)
                                                             .filter(
                                                                 Filters.eq(ID.getFieldName(), workflowExecution.getId()),
                                                                 Filters.eq(CLAIMED_BY_INSTANCE.getFieldName(),
                                                                     morphiaDatastoreProvider.getInstanceId()),
                                                                 Filters.eq(WORKFLOW_STATUS.getFieldName(),
                                                                     WorkflowStatus.RUNNING));

    UpdateResult updateResult = query.update(new UpdateOptions(),
        UpdateOperators.set(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.INQUEUE),
        UpdateOperators.unset(CLAIMED_BY_INSTANCE.getFieldName())
    );
    return updateResult.getModifiedCount() == 1;
  }
}
