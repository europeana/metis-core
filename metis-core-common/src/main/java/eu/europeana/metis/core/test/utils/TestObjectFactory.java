package eu.europeana.metis.core.test.utils;

import eu.europeana.cloud.common.model.dps.States;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.metis.authentication.user.AccountRole;
import eu.europeana.metis.authentication.user.MetisUser;
import eu.europeana.metis.authentication.user.MetisUserAccessToken;
import eu.europeana.metis.core.common.Country;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.Xslt;
import eu.europeana.metis.core.workflow.ScheduleFrequence;
import eu.europeana.metis.core.workflow.ScheduledWorkflow;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPlugin;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-10-04
 */
public class TestObjectFactory {

  public static final int DATASETID = 100;
  public static final String XSLTID = "5a9821af34f04b794dcf63df";
  public static final String EXECUTIONID = "5a5dc67ba458bb00083d49e3";
  public static final String DATASETNAME = "datasetName";
  public static final String WORKFLOWOWNER = "workflowOwner";
  public static final String WORKFLOWNAME = "workflowName";
  public static final String EMAIL = "user.metis@europeana.eu";
  public static final String AUTHORIZATION_HEADER = "Bearer qwerty12345";
  public static final String TOPOLOGY_NAME = "topology_name";
  public static final long EXTERNAL_TASK_ID = 2070373127078497810L;


  private TestObjectFactory() {
  }

  public static Workflow createWorkflowObject() {
    Workflow workflow = new Workflow();
    workflow.setHarvestPlugin(true);
    workflow.setWorkflowOwner(WORKFLOWOWNER);
    workflow.setWorkflowName(WORKFLOWNAME);
    ValidationExternalPluginMetadata validationExternalPluginMetadata = new ValidationExternalPluginMetadata();
    TransformationPluginMetadata transformationPluginMetadata = new TransformationPluginMetadata();
    ValidationInternalPluginMetadata validationInternalPluginMetadata = new ValidationInternalPluginMetadata();

    List<AbstractMetisPluginMetadata> abstractMetisPluginMetadata = new ArrayList<>();
    abstractMetisPluginMetadata.add(validationExternalPluginMetadata);
    abstractMetisPluginMetadata.add(transformationPluginMetadata);
    abstractMetisPluginMetadata.add(validationInternalPluginMetadata);
    workflow.setMetisPluginsMetadata(abstractMetisPluginMetadata);

    return workflow;
  }

  public static List<Workflow> createListOfUserWorkflowsSameOwner(String workflowOwner,
      int size) {
    List<Workflow> workflows = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Workflow workflow = createWorkflowObject();
      workflow.setId(new ObjectId());
      workflow.setWorkflowOwner(workflowOwner);
      workflow.setWorkflowName(String.format("%s%s", WORKFLOWNAME, i));
      workflows.add(workflow);
    }
    return workflows;
  }

  public static WorkflowExecution createWorkflowExecutionObject() {
    Workflow workflow = createWorkflowObject();
    Dataset dataset = createDataset(DATASETNAME);
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    OaipmhHarvestPlugin oaipmhHarvestPlugin = new OaipmhHarvestPlugin(
        new OaipmhHarvestPluginMetadata());
    abstractMetisPlugins.add(oaipmhHarvestPlugin);
    ValidationExternalPlugin validationExternalPlugin = new ValidationExternalPlugin(
        new ValidationExternalPluginMetadata());
    abstractMetisPlugins.add(validationExternalPlugin);

    WorkflowExecution workflowExecution = new WorkflowExecution(dataset,
        workflow, abstractMetisPlugins, 0);
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setCreatedDate(new Date());

    return workflowExecution;
  }

  public static WorkflowExecution createWorkflowExecutionObject(Dataset dataset,
      Workflow workflow) {
    WorkflowExecution workflowExecution = new WorkflowExecution(dataset,
        workflow, new ArrayList<>(), 0);
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setCreatedDate(new Date());

    return workflowExecution;
  }

  public static List<WorkflowExecution> createListOfWorkflowExecutions(int size) {
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Workflow workflow = createWorkflowObject();
      workflow.setId(new ObjectId());
      workflow.setWorkflowName(String.format("%s%s", WORKFLOWNAME, i));
      Dataset dataset = createDataset(String.format("%s%s", DATASETNAME, i));
      dataset.setDatasetId(DATASETID + i);
      WorkflowExecution workflowExecution = createWorkflowExecutionObject(dataset,
          workflow);
      workflowExecution.setId(new ObjectId());
      workflowExecutions.add(workflowExecution);
    }
    return workflowExecutions;
  }

  public static void updateListOfWorkflowExecutionsWithWorkflowStatus(
      List<WorkflowExecution> workflowExecutions, WorkflowStatus workflowStatus) {
    for (WorkflowExecution workflowExecution : workflowExecutions) {
      workflowExecution.setWorkflowStatus(workflowStatus);
    }
  }

  public static ScheduledWorkflow createScheduledWorkflowObject() {
    ScheduledWorkflow scheduledWorkflow = new ScheduledWorkflow();
    scheduledWorkflow.setDatasetId(DATASETID);
    scheduledWorkflow.setWorkflowOwner(WORKFLOWOWNER);
    scheduledWorkflow.setWorkflowName(WORKFLOWNAME);
    scheduledWorkflow.setPointerDate(new Date());
    scheduledWorkflow.setScheduleFrequence(ScheduleFrequence.ONCE);
    scheduledWorkflow.setWorkflowPriority(0);
    return scheduledWorkflow;
  }

  public static List<ScheduledWorkflow> createListOfScheduledWorkflows(int size) {
    List<ScheduledWorkflow> scheduledWorkflows = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
      scheduledWorkflow.setId(new ObjectId());
      scheduledWorkflow.setDatasetId(DATASETID + i);
      scheduledWorkflows.add(scheduledWorkflow);
    }
    return scheduledWorkflows;
  }

  public static List<ScheduledWorkflow> createListOfScheduledWorkflowsWithDateAndFrequence(
      int size, Date date, ScheduleFrequence scheduleFrequence) {
    List<ScheduledWorkflow> scheduledWorkflows = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
      scheduledWorkflow.setId(new ObjectId());
      scheduledWorkflow.setDatasetId(DATASETID + i);
      scheduledWorkflow.setPointerDate(date);
      scheduledWorkflow.setScheduleFrequence(scheduleFrequence);
      scheduledWorkflows.add(scheduledWorkflow);
    }
    return scheduledWorkflows;
  }

  public static Dataset createDataset(String datasetName) {
    Dataset ds = new Dataset();
    ds.setEcloudDatasetId("NOT_CREATED_YET-f525f64c-fea0-44bf-8c56-88f30962734c");
    ds.setDatasetId(DATASETID);
    ds.setDatasetName(datasetName);
    ds.setOrganizationId("1234567890");
    ds.setOrganizationName("OrganizationName");
    ds.setProvider("1234567890");
    ds.setIntermediateProvider("1234567890");
    ds.setDataProvider("1234567890");
    ds.setCreatedByUserId("userId");
    ds.setCreatedDate(new Date());
    ds.setUpdatedDate(new Date());
    ds.setReplacedBy("replacedBy");
    ds.setReplaces("12345");
    ds.setCountry(Country.GREECE);
    ds.setLanguage(Language.AR);
    ds.setDescription("description");
    ds.setNotes("Notes");
    ds.setHarvestingMetadata(new OaipmhHarvestPluginMetadata());
    return ds;
  }

  public static MetisUser createMetisUser(String email) {
    MetisUserAccessToken metisUserAccessToken = new MetisUserAccessToken();
    metisUserAccessToken.setAccessToken("AccessToken_12345");
    metisUserAccessToken.setTimestamp(new Date());

    MetisUser metisUser = new MetisUser();
    metisUser.setEmail(email);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    metisUser.setOrganizationId("Organization_12345");
    metisUser.setOrganizationName("OrganizationName");
    metisUser.setMetisUserFlag(true);
    metisUser.setFirstName("FirstName");
    metisUser.setLastName("LastName");
    metisUser.setUserId("User_12345");
    metisUser.setMetisUserAccessToken(metisUserAccessToken);

    return metisUser;
  }

  public static List<SubTaskInfo> createListOfSubTaskInfo() {
    SubTaskInfo subTaskInfo1 = new SubTaskInfo(1, "some_resource_id1", States.SUCCESS, "",
        "Sensitive Information");
    SubTaskInfo subTaskInfo2 = new SubTaskInfo(2, "some_resource_id1", States.SUCCESS, "",
        "Sensitive Information");
    ArrayList<SubTaskInfo> subTaskInfos = new ArrayList<>();
    subTaskInfos.add(subTaskInfo1);
    subTaskInfos.add(subTaskInfo2);
    return subTaskInfos;
  }

  public static TaskErrorsInfo createTaskErrorsInfoListWithoutIdentifiers(int numberOfErrorTypes) {
    ArrayList<TaskErrorInfo> taskErrorInfos = new ArrayList<>();
    for (int i = 0; i < numberOfErrorTypes; i++) {
      TaskErrorInfo taskErrorInfo = new TaskErrorInfo("be39ef50-f77d-11e7-af0f-fa163e77119a",
          String.format("Error%s", i), 2);
      taskErrorInfos.add(taskErrorInfo);
    }
    return new TaskErrorsInfo(EXTERNAL_TASK_ID, taskErrorInfos);
  }

  public static TaskErrorsInfo createTaskErrorsInfoListWithIdentifiers(int numberOfErrorTypes) {
    ArrayList<TaskErrorInfo> taskErrorInfos = new ArrayList<>();
    for (int i = 0; i < numberOfErrorTypes; i++) {
      TaskErrorInfo taskErrorInfo = new TaskErrorInfo("be39ef50-f77d-11e7-af0f-fa163e77119a",
          String.format("Error%s", i), 2);
      ArrayList<String> identifiers = new ArrayList<>();
      identifiers.add("identifier1");
      identifiers.add("identifier2");
      taskErrorInfo.setIdentifiers(identifiers);
      taskErrorInfos.add(taskErrorInfo);
    }
    return new TaskErrorsInfo(EXTERNAL_TASK_ID, taskErrorInfos);
  }

  public static TaskErrorsInfo createTaskErrorsInfoWithIdentifiers(String errorType,
      String message) {
    ArrayList<String> identifiers1 = new ArrayList<>();
    identifiers1.add("identifier1");
    identifiers1.add("identifier2");
    TaskErrorInfo taskErrorInfo1 = new TaskErrorInfo(errorType,
        message, 2, identifiers1);
    ArrayList<TaskErrorInfo> taskErrorInfos = new ArrayList<>();
    taskErrorInfos.add(taskErrorInfo1);

    return new TaskErrorsInfo(EXTERNAL_TASK_ID, taskErrorInfos);
  }

  public static Xslt createXslt(Dataset dataset) {
    return new Xslt(dataset.getDatasetId(), "<xslt attribute:\"value\"></xslt>");
  }
}

