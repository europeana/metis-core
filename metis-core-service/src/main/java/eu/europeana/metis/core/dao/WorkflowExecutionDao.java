package eu.europeana.metis.core.dao;

import static eu.europeana.metis.core.common.DaoFieldNames.CREATED_DATE;
import static eu.europeana.metis.core.common.DaoFieldNames.DATASET_ID;
import static eu.europeana.metis.core.common.DaoFieldNames.FINISHED_DATE;
import static eu.europeana.metis.core.common.DaoFieldNames.ID;
import static eu.europeana.metis.core.common.DaoFieldNames.METIS_PLUGINS;
import static eu.europeana.metis.core.common.DaoFieldNames.PLUGIN_METADATA;
import static eu.europeana.metis.core.common.DaoFieldNames.PLUGIN_STATUS;
import static eu.europeana.metis.core.common.DaoFieldNames.PLUGIN_TYPE;
import static eu.europeana.metis.core.common.DaoFieldNames.STARTED_DATE;
import static eu.europeana.metis.core.common.DaoFieldNames.WORKFLOW_STATUS;
import static eu.europeana.metis.core.common.DaoFieldNames.XSLT_ID;
import static eu.europeana.metis.network.ExternalRequestUtil.retryableExternalRequestForNetworkExceptions;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.DeleteOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.expressions.ArrayExpressions;
import dev.morphia.aggregation.expressions.ComparisonExpressions;
import dev.morphia.aggregation.expressions.ConditionalExpressions;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.MathExpressions;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.MathExpression;
import dev.morphia.aggregation.stages.Lookup;
import dev.morphia.aggregation.stages.Projection;
import dev.morphia.aggregation.stages.Sort;
import dev.morphia.aggregation.stages.Unwind;
import dev.morphia.annotations.Entity;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eu.europeana.metis.authentication.user.MetisUserView;
import eu.europeana.metis.core.common.DaoFieldNames;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.rest.RequestLimits;
import eu.europeana.metis.core.workflow.SystemId;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.mongo.utils.MorphiaUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 * Data Access Object for workflow executions using mongo.
 *
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-05-26
 */
@Repository
public class WorkflowExecutionDao implements MetisDao<WorkflowExecution, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowExecutionDao.class);

  private static final int INQUEUE_POSITION_IN_OVERVIEW = 1;
  private static final int RUNNING_POSITION_IN_OVERVIEW = 2;
  private static final int DEFAULT_POSITION_IN_OVERVIEW = 3;

  private final MorphiaDatastoreProvider morphiaDatastoreProvider;
  private int workflowExecutionsPerRequest =
      RequestLimits.WORKFLOW_EXECUTIONS_PER_REQUEST.getLimit();
  private int maxServedExecutionListLength = Integer.MAX_VALUE;

  /**
   * Constructs the DAO
   *
   * @param morphiaDatastoreProvider {@link MorphiaDatastoreProvider} used to access Mongo
   */
  @Autowired
  public WorkflowExecutionDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    this.morphiaDatastoreProvider = morphiaDatastoreProvider;
  }

  @Override
  public WorkflowExecution create(WorkflowExecution workflowExecution) {
    final ObjectId objectId = Optional.ofNullable(workflowExecution.getId())
                                      .orElseGet(ObjectId::new);
    workflowExecution.setId(objectId);
    final WorkflowExecution workflowExecutionSaved = retryableExternalRequestForNetworkExceptions(
        () -> morphiaDatastoreProvider.getDatastore().save(workflowExecution));
    LOGGER.debug("WorkflowExecution for datasetId '{}' created in Mongo",
        workflowExecution.getDatasetId());
    return workflowExecutionSaved;
  }

  @Override
  public String update(WorkflowExecution workflowExecution) {
    final WorkflowExecution workflowExecutionSaved = retryableExternalRequestForNetworkExceptions(
        () ->
            morphiaDatastoreProvider.getDatastore().save(workflowExecution));
    LOGGER.debug("WorkflowExecution for datasetId '{}' updated in Mongo",
        workflowExecution.getDatasetId());
    return workflowExecutionSaved == null ? null : workflowExecutionSaved.getId().toString();
  }

  /**
   * Overwrites only the portion of the WorkflowExecution that contains the plugins.
   *
   * @param workflowExecution the WorkflowExecution to update
   */
  public void updateWorkflowPlugins(WorkflowExecution workflowExecution) {
    Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
                                                             .find(WorkflowExecution.class)
                                                             .filter(Filters.eq(ID.getFieldName(), workflowExecution.getId()));

    final UpdateOperator updateOperator = UpdateOperators
        .set(METIS_PLUGINS.getFieldName(), workflowExecution.getMetisPlugins());

    UpdateResult updateResult = retryableExternalRequestForNetworkExceptions(
        () -> query.update(new UpdateOptions(), updateOperator));
    LOGGER.debug(
        "WorkflowExecution metisPlugins for datasetId '{}' updated in Mongo. (UpdateResults: {})",
        workflowExecution.getDatasetId(),
        updateResult == null ? 0 : updateResult.getModifiedCount());
  }

  /**
   * Overwrites only the portion of the WorkflowExecution that contains the monitor information(plugins, started date, updated
   * date).
   *
   * @param workflowExecution the WorkflowExecution to update
   */
  public void updateMonitorInformation(WorkflowExecution workflowExecution) {
    Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
                                                             .find(WorkflowExecution.class)
                                                             .filter(Filters.eq(ID.getFieldName(), workflowExecution.getId()));
    final ArrayList<UpdateOperator> updateOperators = new ArrayList<>();
    updateOperators.add(UpdateOperators
        .set(WORKFLOW_STATUS.getFieldName(),
            workflowExecution.getWorkflowStatus()));
    if (workflowExecution.getStartedDate() != null) {
      updateOperators
          .add(UpdateOperators.set("startedDate", workflowExecution.getStartedDate()));
    }
    if (workflowExecution.getUpdatedDate() != null) {
      updateOperators
          .add(UpdateOperators.set("updatedDate", workflowExecution.getUpdatedDate()));
    }
    updateOperators.add(
        UpdateOperators.set(METIS_PLUGINS.getFieldName(), workflowExecution.getMetisPlugins()));
    UpdateResult updateResult = retryableExternalRequestForNetworkExceptions(
        () -> query.update(new UpdateOptions(), updateOperators.toArray(UpdateOperator[]::new)));
    LOGGER.debug(
        "WorkflowExecution monitor information for datasetId '{}' updated in Mongo. (UpdateResults: {})",
        workflowExecution.getDatasetId(),
        updateResult == null ? 0 : updateResult.getModifiedCount());
  }

  /**
   * Set the cancelling field in the database.
   * <p>Also adds information of the user identifier that cancelled the execution or if it was by a
   * system operation, using {@link SystemId} values as identifiers. For historical executions the value of the
   * <code>cancelledBy</code> field will remain <code>null</code></p>
   *
   * @param workflowExecution the workflowExecution to be cancelled
   * @param metisUserView the user that triggered the cancellation or null if it was the system
   */
  public void setCancellingState(WorkflowExecution workflowExecution, MetisUserView metisUserView) {
    Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
                                                             .find(WorkflowExecution.class)
                                                             .filter(Filters.eq(ID.getFieldName(), workflowExecution.getId()));
    String cancelledBy;
    if (metisUserView == null || metisUserView.getUserId() == null) {
      cancelledBy = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name();
    } else {
      cancelledBy = metisUserView.getUserId();
    }
    final UpdateOperator setCancellingOperator = UpdateOperators.set("cancelling", Boolean.TRUE);
    final UpdateOperator setCancelledByOperator = UpdateOperators.set("cancelledBy", cancelledBy);

    UpdateResult updateResult = retryableExternalRequestForNetworkExceptions(
        () -> query.update(new UpdateOptions(), setCancellingOperator, setCancelledByOperator));
    LOGGER.debug(
        "WorkflowExecution cancelling for datasetId '{}' set to true in Mongo. (UpdateResults: {})",
        workflowExecution.getDatasetId(),
        updateResult == null ? 0 : updateResult.getModifiedCount());
  }

  @Override
  public WorkflowExecution getById(String id) {
    Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
                                                             .find(WorkflowExecution.class)
                                                             .filter(Filters.eq(ID.getFieldName(), new ObjectId(id)));
    return retryableExternalRequestForNetworkExceptions(query::first);
  }

  @Override
  public boolean delete(WorkflowExecution workflowExecution) {
    return false;
  }

  /**
   * Get the WorkflowExecution for a dataset identifier that is {@link WorkflowStatus#INQUEUE} or {@link WorkflowStatus#RUNNING}
   *
   * @param datasetId the dataset identifier
   * @return the WorkflowExecution if found
   */
  public WorkflowExecution getRunningOrInQueueExecution(String datasetId) {
    Query<WorkflowExecution> query = runningOrInqueueQuery(datasetId);
    return retryableExternalRequestForNetworkExceptions(query::first);
  }

  /**
   * Check the existence of a WorkflowExecution in the database.
   *
   * @param workflowExecution the WorkflowExecution to check upon
   * @return true if it exist, false if it does not exist
   */
  public boolean exists(WorkflowExecution workflowExecution) {
    return retryableExternalRequestForNetworkExceptions(
        () -> morphiaDatastoreProvider.getDatastore().find(WorkflowExecution.class)
                                      .filter(Filters.eq(DATASET_ID.getFieldName(), workflowExecution.getDatasetId()))
                                      .first(new FindOptions().projection().include(ID.getFieldName()))) != null;
  }

  /**
   * Check if a WorkflowExecution exists for a dataset identifier and has not completed it's execution.
   *
   * @param datasetId the dataset identifier
   * @return the identifier of the execution if found, otherwise null
   */
  public String existsAndNotCompleted(String datasetId) {
    Query<WorkflowExecution> query = runningOrInqueueQuery(datasetId);

    final FindOptions findOptions = new FindOptions();
    findOptions.projection().include(ID.getFieldName());
    findOptions.projection().include(WORKFLOW_STATUS.getFieldName());

    WorkflowExecution storedWorkflowExecution = retryableExternalRequestForNetworkExceptions(
        () -> query.first(findOptions));
    if (storedWorkflowExecution != null) {
      return storedWorkflowExecution.getId().toString();
    }
    return null;
  }

  private Query<WorkflowExecution> runningOrInqueueQuery(String datasetId) {
    Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
                                                             .find(WorkflowExecution.class);

    final Filter datasetIdFilter = Filters.eq(DATASET_ID.getFieldName(), datasetId);
    final Filter workflowStatusFilter = Filters
        .or(Filters.eq(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.INQUEUE),
            Filters.eq(WORKFLOW_STATUS.getFieldName(), WorkflowStatus.RUNNING));
    query.filter(datasetIdFilter, workflowStatusFilter);

    return query;
  }

  /**
   * Get the first successful Plugin of a WorkflowExecution for a dataset identifier and a set of plugin types
   *
   * @param datasetId the dataset identifier
   * @param pluginTypes the set of plugin types to check for. Cannot be null or contain null values.
   * @return the first plugin found
   */
  public PluginWithExecutionId<MetisPlugin> getFirstSuccessfulPlugin(String datasetId,
      Set<PluginType> pluginTypes) {
    return Optional.ofNullable(getFirstOrLastFinishedPlugin(datasetId, pluginTypes, true))
                   .orElse(null);
  }

  /**
   * Get the last successful Plugin of a WorkflowExecution for a dataset identifier and a set of plugin types
   *
   * @param datasetId the dataset identifier
   * @param pluginTypes the set of plugin types to check for. Cannot be null or contain null values.
   * @return the last plugin found
   */
  public PluginWithExecutionId<MetisPlugin> getLatestSuccessfulPlugin(String datasetId,
      Set<PluginType> pluginTypes) {
    return Optional.ofNullable(getFirstOrLastFinishedPlugin(datasetId, pluginTypes, false))
                   .orElse(null);
  }

  /**
   * Get the last successful Plugin of a WorkflowExecution for a dataset identifier and a set of plugin types
   *
   * @param datasetId the dataset identifier
   * @param pluginTypes the set of plugin types to check for. Cannot be null or contain null values.
   * @param limitToValidData Only return the result if it has valid data (see {@link DataStatus}).
   * @return the last plugin found
   */
  public PluginWithExecutionId<ExecutablePlugin> getLatestSuccessfulExecutablePlugin(
      String datasetId,
      Set<ExecutablePluginType> pluginTypes, boolean limitToValidData) {

    // Verify the plugin types
    verifyEnumSetIsValidAndNotEmpty(pluginTypes);

    // Perform the database query. If nothing found, we are done.
    final Set<PluginType> convertedPluginTypes = pluginTypes.stream()
                                                            .map(ExecutablePluginType::toPluginType).collect(Collectors.toSet());
    final PluginWithExecutionId<MetisPlugin> uncastResultWrapper =
        getFirstOrLastFinishedPlugin(datasetId, convertedPluginTypes, false);
    final MetisPlugin uncastResult = Optional.ofNullable(uncastResultWrapper)
                                             .map(PluginWithExecutionId::getPlugin).orElse(null);
    if (uncastResultWrapper == null || uncastResult == null) {
      return null;
    }

    // Check for the result type: it should be executable.
    if (!(uncastResult instanceof ExecutablePlugin)) {
      LOGGER.warn("Found plugin {} for executable plugin type {} that is not itself executable.",
          uncastResult.getId(), uncastResult.getPluginType());
      return null;
    }
    final ExecutablePlugin castResult = (ExecutablePlugin) uncastResult;

    // if necessary, check for the data validity.
    final PluginWithExecutionId<ExecutablePlugin> result;
    if (limitToValidData && MetisPlugin.getDataStatus(castResult) != DataStatus.VALID) {
      result = null;
    } else {
      result = new PluginWithExecutionId<>(uncastResultWrapper.getExecutionId(), castResult);
    }
    return result;
  }

  PluginWithExecutionId<MetisPlugin> getFirstOrLastFinishedPlugin(String datasetId,
      Set<PluginType> pluginTypes, boolean firstFinished) {

    // Verify the plugin types
    verifyEnumSetIsValidAndNotEmpty(pluginTypes);

    // Create the filter to match a plugin satisfying the conditions.
    final Filter datasetIdFilter = Filters.eq(DATASET_ID.getFieldName(), datasetId);
    final Filter pluginStatusFilter = Filters
        .eq(METIS_PLUGINS.getFieldName() + "." + PLUGIN_STATUS.getFieldName(),
            PluginStatus.FINISHED);

    List<Filter> pluginTypesFilters = new ArrayList<>(pluginTypes.size());
    final String pluginTypeField = METIS_PLUGINS.getFieldName() + "." + PLUGIN_TYPE.getFieldName();
    for (PluginType pluginType : pluginTypes) {
      pluginTypesFilters.add(Filters.eq(pluginTypeField, pluginType));
    }
    final Filter collectedFilters;
    if (pluginTypesFilters.isEmpty()) {
      collectedFilters = Filters.and(datasetIdFilter, pluginStatusFilter);
    } else {
      final Filter pluginTypeOrFilter = Filters.or(pluginTypesFilters.toArray(Filter[]::new));
      collectedFilters = Filters.and(datasetIdFilter, pluginStatusFilter, pluginTypeOrFilter);
    }

    // Query: unwind and match again so that we know that all conditions apply to the same plugin.
    final Aggregation<WorkflowExecution> aggregation = morphiaDatastoreProvider.getDatastore()
                                                                               .aggregate(WorkflowExecution.class);

    final String orderField =
        METIS_PLUGINS.getFieldName() + "." + FINISHED_DATE.getFieldName();
    aggregation.match(collectedFilters)
               .unwind(Unwind.unwind(METIS_PLUGINS.getFieldName()))
               .match(collectedFilters)
               .sort(firstFinished ? Sort.sort().ascending(orderField) : Sort.sort().descending(orderField))
               .limit(1);

    final List<WorkflowExecution> metisPluginsIterator = MorphiaUtils
        .getListOfAggregationRetryable(aggregation,
            WorkflowExecution.class);

    // Because of the unwind, we know that the plugin we need is always the first one.
    return Optional.ofNullable(metisPluginsIterator).stream().flatMap(Collection::stream)
                   .filter(execution -> !execution.getMetisPlugins().isEmpty())
                   .map(execution -> new PluginWithExecutionId<MetisPlugin>(execution,
                       execution.getMetisPlugins().getFirst())).findFirst().orElse(null);
  }

  private void verifyEnumSetIsValidAndNotEmpty(Set<? extends Enum<?>> set) {
    if (set == null || set.isEmpty() || set.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Get all WorkflowExecutions paged.
   *
   * @param datasetIds a set of dataset identifiers to filter, can be empty or null to get all
   * @param workflowStatuses a set of workflow statuses to filter, can be empty or null
   * @param orderField the field to be used to sort the results
   * @param ascending a boolean value to request the ordering to ascending or descending
   * @param nextPage The first page to be served (zero-based)
   * @param pageCount How many pages are requested - can be null
   * @param ignoreMaxServedExecutionsLimit whether this method is to apply the limit on the number of executions are served. Be
   * careful when setting this to true.
   * @return a list of all the WorkflowExecutions found
   */
  public ResultList<WorkflowExecution> getAllWorkflowExecutions(Set<String> datasetIds,
      Set<WorkflowStatus> workflowStatuses, DaoFieldNames orderField, boolean ascending,
      int nextPage, Integer pageCount, boolean ignoreMaxServedExecutionsLimit) {

    // Prepare pagination and check that there is something to query
    final Pagination pagination = createPagination(nextPage, pageCount, ignoreMaxServedExecutionsLimit);
    if (pagination.getLimit() < 1) {
      return createResultList(Collections.emptyList(), pagination);
    }

    // Create query
    final Query<WorkflowExecution> query =
        morphiaDatastoreProvider.getDatastore().find(WorkflowExecution.class);

    // Set dataset ID and workflow status limitations.
    if (datasetIds != null && !datasetIds.isEmpty()) {
      query.filter(Filters.in(DATASET_ID.getFieldName(), datasetIds));
    }
    if (!CollectionUtils.isEmpty(workflowStatuses)) {
      query.filter(Filters.in(WORKFLOW_STATUS.getFieldName(), workflowStatuses));
    }

    // Execute query with correct pagination
    final FindOptions findOptions = new FindOptions().skip(pagination.getSkip())
                                                     .limit(pagination.getLimit());

    // Set ordering
    if (orderField != null) {
      if (ascending) {
        findOptions.sort(dev.morphia.query.Sort.ascending(orderField.getFieldName()));
      } else {
        findOptions.sort(dev.morphia.query.Sort.descending(orderField.getFieldName()));
      }
    }

    final List<WorkflowExecution> result = MorphiaUtils.getListOfQueryRetryable(query, findOptions);
    return createResultList(result, pagination);
  }

  /**
   * Get an overview of all WorkflowExecutions. This returns a list of executions ordered to display an overview. First the ones
   * in queue, then those in progress and then those that are finalized. Within these categories they will be sorted by creation
   * date (most recent first). This method does support pagination.
   * <p>
   * TODO when we migrate
   * to mongo 3.4 or later, we can do this easier with new aggregation pipeline stages and
   * operators. The main improvements are 1) to try to map the root to the 'execution' variable so
   * that we don't have to look it up afterwards, and 2) to use $addFields with $switch to add the
   * statusIndex instead of having to go through creating and subtracting the two temporary fields.
   *
   * @param datasetIds a set of dataset identifiers to filter, can be empty or null to get all
   * @param pluginStatuses the plugin statuses to filter. Can be null.
   * @param pluginTypes the plugin types to filter. Can be null.
   * @param fromDate the date from where the results should start. Can be null.
   * @param toDate the date to where the results should end. Can be null.
   * @param nextPage the nextPage token
   * @param pageCount the number of pages that are requested
   * @return a list of all the WorkflowExecutions found. Is not null.
   */
  public ResultList<ExecutionDatasetPair> getWorkflowExecutionsOverview(Set<String> datasetIds,
      Set<PluginStatus> pluginStatuses, Set<PluginType> pluginTypes, Date fromDate, Date toDate,
      int nextPage, int pageCount) {
    return getWorkflowExecutionsOverview(datasetIds, pluginStatuses, pluginTypes, fromDate, toDate,
        createPagination(nextPage, pageCount, false));
  }

  ResultList<ExecutionDatasetPair> getWorkflowExecutionsOverview(Set<String> datasetIds,
      Set<PluginStatus> pluginStatuses, Set<PluginType> pluginTypes, Date fromDate, Date toDate,
      Pagination pagination) {

    return retryableExternalRequestForNetworkExceptions(() -> {

      // Prepare pagination and check that there is something to query
      if (pagination.getLimit() < 1) {
        return createResultList(Collections.emptyList(), pagination);
      }

      // Create the aggregate pipeline
      final Aggregation<WorkflowExecution> aggregation = morphiaDatastoreProvider.getDatastore()
                                                                                 .aggregate(WorkflowExecution.class);

      // Step 1: create filter to match
      final Filter filter = createFilter(datasetIds, pluginStatuses, pluginTypes, fromDate, toDate);
      aggregation.match(filter);

      // Step 2: determine status index field
      final String statusIndexField = determineOrderingStatusIndex(aggregation);

      // Step 3: Sort - first on the status index, then on the createdDate.
      aggregation
          .sort(Sort.sort().ascending(statusIndexField).descending(CREATED_DATE.getFieldName()));

      // Step 4: Apply pagination
      aggregation.skip(pagination.getSkip()).limit(pagination.getLimit());

      // Step 5: Create join of dataset and execution to combine the data information
      joinDatasetAndWorkflowExecution(aggregation);

      // Done: execute and return result.
      final List<ExecutionDatasetPair> result = MorphiaUtils
          .getListOfAggregationRetryable(aggregation,
              ExecutionDatasetPair.class);
      return createResultList(result, pagination);
    });
  }

  private Filter createFilter(Set<String> datasetIds, Set<PluginStatus> pluginStatuses,
      Set<PluginType> pluginTypes, Date fromDate, Date toDate) {
    List<Filter> elemMatchFilters = new ArrayList<>();
    if (!CollectionUtils.isEmpty(pluginTypes)) {
      elemMatchFilters.add(Filters.in(PLUGIN_TYPE.getFieldName(), pluginTypes));
    }
    if (!CollectionUtils.isEmpty(pluginStatuses)) {
      elemMatchFilters.add(Filters.in(PLUGIN_STATUS.getFieldName(), pluginStatuses));
    }
    if (fromDate != null) {
      elemMatchFilters.add(Filters.gte(STARTED_DATE.getFieldName(), fromDate));
    }
    if (toDate != null) {
      elemMatchFilters.add(Filters.lt(STARTED_DATE.getFieldName(), toDate));
    }
    final Filter elemMatchFilter = Filters
        .elemMatch(METIS_PLUGINS.getFieldName(), elemMatchFilters.toArray(Filter[]::new));

    final Filter collectedFilters;
    if (CollectionUtils.isEmpty(datasetIds)) {
      collectedFilters = elemMatchFilter;
    } else {
      final Filter datasetIdFilter = Filters.in(DATASET_ID.getFieldName(), datasetIds);
      collectedFilters = Filters.and(elemMatchFilter, datasetIdFilter);
    }
    return collectedFilters;
  }

  private String determineOrderingStatusIndex(Aggregation<WorkflowExecution> aggregation) {
    // Step 1: Add specific positions when the status is INQUEUE or RUNNING.
    final String statusInQueueField = "statusInQueue";
    final String statusRunningField = "statusRunning";
    final Expression inqueueCheckExpression = ComparisonExpressions
        .eq(Expressions.field(WORKFLOW_STATUS.getFieldName()),
            Expressions.value(WorkflowStatus.INQUEUE.name()));
    final Expression inqueueConditionExpression = ConditionalExpressions
        .condition(inqueueCheckExpression, Expressions.value(INQUEUE_POSITION_IN_OVERVIEW),
            Expressions.value(0));
    final Expression runningCheckExpression = ComparisonExpressions
        .eq(Expressions.field(WORKFLOW_STATUS.getFieldName()),
            Expressions.value(WorkflowStatus.RUNNING.name()));
    final Expression runningConditionExpression = ConditionalExpressions
        .condition(runningCheckExpression, Expressions.value(RUNNING_POSITION_IN_OVERVIEW),
            Expressions.value(0));

    aggregation.project(Projection.project()
                                  .include(statusInQueueField, inqueueConditionExpression)
                                  .include(statusRunningField, runningConditionExpression)
                                  .include(CREATED_DATE.getFieldName())
                                  .include(DATASET_ID.getFieldName()));

    // Step 2: Copy specific positions to final variable: use default position if no position is set.
    final String statusIndexField = "statusIndex";

    final MathExpression sumExpression = MathExpressions
        .add(Expressions.field(statusInQueueField), Expressions.field(statusRunningField));
    final Expression sumCheckExpression = ComparisonExpressions
        .eq(sumExpression, Expressions.value(0));
    final Expression statusIndexExpression = ConditionalExpressions
        .condition(sumCheckExpression, Expressions.value(DEFAULT_POSITION_IN_OVERVIEW),
            sumExpression);

    aggregation.project(Projection.project()
                                  .include(statusIndexField, statusIndexExpression)
                                  .include(CREATED_DATE.getFieldName())
                                  .include(DATASET_ID.getFieldName()));

    return statusIndexField;
  }

  private void joinDatasetAndWorkflowExecution(Aggregation<WorkflowExecution> aggregation) {
    // Step 1: Join with the dataset and the execution
    final String datasetListField = "datasetList";
    final String executionListField = "executionList";
    aggregation.lookup(Lookup.lookup(Dataset.class).localField(DATASET_ID.getFieldName())
                             .foreignField(DATASET_ID.getFieldName()).as(datasetListField));
    aggregation.lookup(Lookup.lookup(WorkflowExecution.class).localField(ID.getFieldName())
                             .foreignField(ID.getFieldName()).as(executionListField));

    // Step 2: Keep only the first entry in the dataset and execution lists.
    final String datasetField = "dataset";
    final String executionField = "execution";
    final Projection projection = Projection.project()
                                            .include(datasetField,
                                                ArrayExpressions.elementAt(Expressions.field(datasetListField),
                                                    Expressions.value(0)))
                                            .include(executionField, ArrayExpressions
                                                .elementAt(Expressions.field(executionListField), Expressions.value(0)))
                                            .suppressId();
    aggregation.project(projection);
  }

  /**
   * This object contains a pair consisting of a dataset and an execution. It is meant to be a result of aggregate queries, so the
   * field names cannot easily be changed.
   * <p>Annotation {@link Entity} required so that morphia can handle the aggregations.</p>
   */
  @Entity
  public static class ExecutionDatasetPair {

    private Dataset dataset;
    private WorkflowExecution execution;

    public ExecutionDatasetPair() {
    }

    /**
     * Constructor.
     *
     * @param dataset The dataset.
     * @param execution The execution.
     */
    public ExecutionDatasetPair(Dataset dataset, WorkflowExecution execution) {
      this.dataset = dataset;
      this.execution = execution;
    }

    public Dataset getDataset() {
      return dataset;
    }

    public WorkflowExecution getExecution() {
      return execution;
    }
  }

  /**
   * The number of WorkflowExecutions that would be returned if a get all request would be performed.
   *
   * @return the number representing the size during a get all request
   */
  public int getWorkflowExecutionsPerRequest() {
    synchronized (this) {
      return workflowExecutionsPerRequest;
    }
  }

  /**
   * Set the number of WorkflowExecutions that would be returned if a get all request would be performed.
   *
   * @param workflowExecutionsPerRequest the number to set to
   */
  public void setWorkflowExecutionsPerRequest(int workflowExecutionsPerRequest) {
    synchronized (this) {
      this.workflowExecutionsPerRequest = workflowExecutionsPerRequest;
    }
  }

  /**
   * Get the maximum number of workflow executions that are served (regardless of pagination).
   *
   * @return The limit.
   */
  public int getMaxServedExecutionListLength() {
    synchronized (this) {
      return maxServedExecutionListLength;
    }
  }

  /**
   * Set the maximum number of workflowExecutions that are served (regardless of pagination).
   *
   * @param maxServedExecutionListLength The limit.
   */
  public void setMaxServedExecutionListLength(int maxServedExecutionListLength) {
    synchronized (this) {
      this.maxServedExecutionListLength = maxServedExecutionListLength;
    }
  }

  /**
   * Check if a WorkflowExecution using an execution identifier is {@link WorkflowStatus#CANCELLED}
   *
   * @param id the execution identifier
   * @return true for cancelled, false for not cancelled
   */
  public boolean isCancelled(ObjectId id) {
    WorkflowExecution workflowExecution = retryableExternalRequestForNetworkExceptions(() ->
        morphiaDatastoreProvider.getDatastore().find(WorkflowExecution.class)
                                .filter(Filters.eq(ID.getFieldName(), id))
                                .first(new FindOptions().projection().include(WORKFLOW_STATUS.getFieldName())));
    return workflowExecution != null
        && workflowExecution.getWorkflowStatus() == WorkflowStatus.CANCELLED;
  }

  /**
   * Check if a WorkflowExecution using an execution identifier is in a cancelling state. The state before finally being
   * {@link WorkflowStatus#CANCELLED}
   *
   * @param id the execution identifier
   * @return true for cancelling, false for not cancelling
   */
  public boolean isCancelling(ObjectId id) {
    WorkflowExecution workflowExecution = retryableExternalRequestForNetworkExceptions(
        () -> morphiaDatastoreProvider.getDatastore().find(WorkflowExecution.class)
                                      .filter(Filters.eq(ID.getFieldName(), id))
                                      .first(new FindOptions().projection().include("cancelling")));
    return workflowExecution != null && workflowExecution.isCancelling();
  }

  /**
   * Delete all WorkflowExecutions for a dataset identifier
   *
   * @param datasetId the dataset identifier
   * @return true if at least one was removed
   */
  public boolean deleteAllByDatasetId(String datasetId) {
    Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
                                                             .find(WorkflowExecution.class);
    query.filter(Filters.eq(DATASET_ID.getFieldName(), datasetId));
    DeleteResult deleteResult = retryableExternalRequestForNetworkExceptions(
        () -> query.delete(new DeleteOptions().multi(true)));
    LOGGER.debug("WorkflowExecution with datasetId: {}, deleted from Mongo", datasetId);
    return (deleteResult == null ? 0 : deleteResult.getDeletedCount()) >= 1;
  }

  /**
   * This method retrieves the workflow execution of which the task with the given ID is a subtask.
   *
   * @param externalTaskId The external task ID that is to be queried.
   * @return The workflow execution.
   */
  public WorkflowExecution getByExternalTaskId(long externalTaskId) {
    // TODO JV Validation is disabled because otherwise it complains that the subquery is looking in a
    // list of AbstractMetisPlugin objects that don't have the "externalTaskId" property being queried.
    final Query<WorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
                                                                   .find(WorkflowExecution.class).disableValidation();
    query.filter(Filters.elemMatch(METIS_PLUGINS.getFieldName(),
        Filters.eq("externalTaskId", Long.toString(externalTaskId))));
    return retryableExternalRequestForNetworkExceptions(query::first);
  }

  /**
   * This method retrieves the workflow execution that contains a subtask satisfying the given parameters.
   *
   * @param plugin The plugin ID representing the subtask.
   * @param datasetId The dataset ID of the workflow execution.
   * @return The workflow execution.
   */
  public WorkflowExecution getByTaskExecution(ExecutedMetisPluginId plugin, String datasetId) {

    // Create subquery to find the correct plugin.
    List<Filter> elemMatchFilters = new ArrayList<>();
    elemMatchFilters.add(Filters.eq(STARTED_DATE.getFieldName(), plugin.getPluginStartedDate()));
    elemMatchFilters.add(Filters.eq(PLUGIN_TYPE.getFieldName(), plugin.getPluginType()));

    // Create query to find workflow execution
    final Query<WorkflowExecution> query =
        morphiaDatastoreProvider.getDatastore().find(WorkflowExecution.class);
    query.filter(Filters.eq(DATASET_ID.getFieldName(), datasetId));
    query.filter(Filters.elemMatch(METIS_PLUGINS.getFieldName(),
        elemMatchFilters.toArray(Filter[]::new)));
    return retryableExternalRequestForNetworkExceptions(query::first);
  }

  /**
   * This method retrieves any {@link WorkflowExecution} that matches the provided {@code xsltId}.
   *
   * @param xsltId the xslt id
   * @return the matched workflow execution
   */
  public WorkflowExecution getAnyByXsltId(String xsltId) {
    // Create query to find workflow execution
    final Query<WorkflowExecution> query =
        morphiaDatastoreProvider.getDatastore().find(WorkflowExecution.class)
                                .disableValidation();
    query.disableValidation().filter(Filters.elemMatch(METIS_PLUGINS.getFieldName(),
        Filters.eq(PLUGIN_METADATA.getFieldName() + "." + XSLT_ID.getFieldName(), xsltId)));
    return retryableExternalRequestForNetworkExceptions(query::first);
  }

  Pagination createPagination(int firstPage, Integer pageCount,
      boolean ignoreMaxServedExecutionsLimit) {

    // Compute the total number (including skipped pages)
    final int pageSize = getWorkflowExecutionsPerRequest();
    final int maxResultCount =
        ignoreMaxServedExecutionsLimit ? Integer.MAX_VALUE : getMaxServedExecutionListLength();
    int total = maxResultCount; //Default value if no pageCount supplied
    if (pageCount != null) {
      total = Math.min((firstPage + pageCount) * pageSize, maxResultCount);
    }

    // Compute the skipped result count and the returned result count (limit).
    final int skip = firstPage * pageSize;
    final boolean maxRequested = total == maxResultCount;
    final int limit = Math.max(total - skip, 0);
    return new Pagination(skip, limit, maxRequested);
  }

  static class Pagination {

    private final int skip;
    private final int limit;
    private final boolean maxRequested;

    private Pagination(int skip, int limit, boolean maxRequested) {
      this.skip = skip;
      this.limit = limit;
      this.maxRequested = maxRequested;
    }

    int getSkip() {
      return skip;
    }

    int getLimit() {
      return limit;
    }

    boolean isMaxReached(int resultSize) {
      return maxRequested && resultSize == limit;
    }
  }

  private static <T> ResultList<T> createResultList(List<T> result, Pagination pagination) {
    return new ResultList<>(result, pagination.isMaxReached(result.size()));
  }

  /**
   * This object contains a result list with some pagination information.
   *
   * @param <T> The type of the result objects.
   */
  public static class ResultList<T> {

    private final List<T> results;
    private final boolean maxResultCountReached;

    /**
     * Constructor.
     *
     * @param results The results.
     * @param maxResultCountReached Whether the maximum result count has been reached (indicating whether next pages will be
     * served).
     */
    public ResultList(List<T> results, boolean maxResultCountReached) {
      this.results = new ArrayList<>(results);
      this.maxResultCountReached = maxResultCountReached;
    }

    public List<T> getResults() {
      return Collections.unmodifiableList(results);
    }

    public boolean isMaxResultCountReached() {
      return maxResultCountReached;
    }
  }
}
