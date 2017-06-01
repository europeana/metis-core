package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.europeana.metis.core.workflow.CloudStatistics;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mongodb.morphia.annotations.Indexed;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-05-24
 */
public class VoidMetisPlugin implements AbstractMetisPlugin {
    @Indexed
    private String id;
    private PluginStatus pluginStatus = PluginStatus.INQUEUE;
    private final PluginType pluginType = PluginType.VOID;
    private Map<String, List<String>> parameters = new HashMap<>();

    @Indexed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date startedDate;
    @Indexed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date updatedDate;
    @Indexed
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date finishedDate;

    private ExecutionRecordsStatistics executionRecordsStatistics = new ExecutionRecordsStatistics();

    public VoidMetisPlugin() {
    }

    public VoidMetisPlugin(VoidMetisPluginMetadata voidMetisPluginMetadata)
    {
        if (voidMetisPluginMetadata != null)
            this.parameters = voidMetisPluginMetadata.getParameters();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public PluginStatus getPluginStatus() {
        return pluginStatus;
    }

    @Override
    public void setPluginStatus(PluginStatus pluginStatus) {
        this.pluginStatus = pluginStatus;
    }

    @Override
    public PluginType getPluginType() {
        return pluginType;
    }

    @Override
    public Date getStartedDate() {
        return startedDate;
    }

    @Override
    public void setStartedDate(Date startedDate) {
        this.startedDate = startedDate;
    }

    @Override
    public Date getFinishedDate() {
        return finishedDate;
    }

    @Override
    public void setFinishedDate(Date finishedDate) {
        this.finishedDate = finishedDate;
    }

    @Override
    public Date getUpdatedDate() {
        return updatedDate;
    }

    @Override
    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public ExecutionRecordsStatistics getExecutionRecordsStatistics() {
        return executionRecordsStatistics;
    }

    @Override
    public void setExecutionRecordsStatistics(
        ExecutionRecordsStatistics executionRecordsStatistics) {
        this.executionRecordsStatistics = executionRecordsStatistics;
    }

    @Override
    public void setParameters(Map<String, List<String>> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    @Override
    public void execute() {
    }

    @Override
    public CloudStatistics monitor(String datasetId) {
        return null;
    }

}
