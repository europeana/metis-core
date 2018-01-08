/*
 * Copyright 2007-2013 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.europeana.metis.core.common.Country;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.workflow.HasMongoObjectId;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPluginMetadata;
import eu.europeana.metis.json.ObjectIdSerializer;
import java.util.Date;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

/**
 * Dataset model that contains all the required fields for Dataset functionality.
 * It also contains the harvesting metadata required when executing a harvesting plugin.
 */
@Entity
@Indexes(@Index(fields = {@Field("organizationId"),
    @Field("datasetName")}, options = @IndexOptions(unique = true)))
public class Dataset implements HasMongoObjectId {

  @Id
  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId id;

  @Indexed(options = @IndexOptions(unique = true))
  private String ecloudDatasetId;

  private int datasetId;

  @Indexed
  private String datasetName;

  @Indexed
  private String organizationId;

  @Indexed
  private String organizationName;

  @Indexed
  private String provider;

  @Indexed
  private String intermediateProvider;

  @Indexed
  private String dataProvider;

  @Indexed
  private String createdByUserId;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date createdDate;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date updatedDate;

  private DatasetStatus datasetStatus;

  private String replacedBy;

  private String replaces;

  private Country country;

  private Language language;

  private String description;

  private String notes;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date firstPublishedDate;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date lastPublishedDate;

  private long publishedRecords;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date harvestedDate;

  private long harvestedRecords;

  private AbstractMetisPluginMetadata harvestingMetadata;

  @Override
  public ObjectId getId() {
    return id;
  }

  @Override
  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getEcloudDatasetId() {
    return ecloudDatasetId;
  }

  public void setEcloudDatasetId(String ecloudDatasetId) {
    this.ecloudDatasetId = ecloudDatasetId;
  }

  public int getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(int datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getIntermediateProvider() {
    return intermediateProvider;
  }

  public void setIntermediateProvider(String intermediateProvider) {
    this.intermediateProvider = intermediateProvider;
  }

  public String getDataProvider() {
    return dataProvider;
  }

  public void setDataProvider(String dataProvider) {
    this.dataProvider = dataProvider;
  }

  public String getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(String createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public Date getCreatedDate() {
    return new Date(createdDate.getTime());
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = new Date(createdDate.getTime());
  }

  public Date getUpdatedDate() {
    return new Date(updatedDate.getTime());
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate == null?null:new Date(updatedDate.getTime());
  }

  public DatasetStatus getDatasetStatus() {
    return datasetStatus;
  }

  public void setDatasetStatus(DatasetStatus datasetStatus) {
    this.datasetStatus = datasetStatus;
  }

  public String getReplacedBy() {
    return replacedBy;
  }

  public void setReplacedBy(String replacedBy) {
    this.replacedBy = replacedBy;
  }

  public String getReplaces() {
    return replaces;
  }

  public void setReplaces(String replaces) {
    this.replaces = replaces;
  }

  public Country getCountry() {
    return country;
  }

  public void setCountry(Country country) {
    this.country = country;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Date getFirstPublishedDate() {
    return new Date(firstPublishedDate.getTime());
  }

  public void setFirstPublishedDate(Date firstPublishedDate) {
    this.firstPublishedDate = firstPublishedDate == null?null:new Date(firstPublishedDate.getTime());
  }

  public Date getLastPublishedDate() {
    return new Date(lastPublishedDate.getTime());
  }

  public void setLastPublishedDate(Date lastPublishedDate) {
    this.lastPublishedDate = lastPublishedDate == null?null:new Date(lastPublishedDate.getTime());
  }

  public long getPublishedRecords() {
    return publishedRecords;
  }

  public void setPublishedRecords(long publishedRecords) {
    this.publishedRecords = publishedRecords;
  }

  public Date getHarvestedDate() {
    return new Date(harvestedDate.getTime());
  }

  public void setHarvestedDate(Date harvestedDate) {
    this.harvestedDate = harvestedDate == null?null:new Date(harvestedDate.getTime());
  }

  public long getHarvestedRecords() {
    return harvestedRecords;
  }

  public void setHarvestedRecords(long harvestedRecords) {
    this.harvestedRecords = harvestedRecords;
  }

  public AbstractMetisPluginMetadata getHarvestingMetadata() {
    return harvestingMetadata;
  }

  public void setHarvestingMetadata(
      AbstractMetisPluginMetadata harvestingMetadata) {
    this.harvestingMetadata = harvestingMetadata;
  }
}
