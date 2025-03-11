package eu.europeana.metis.core.dataset;

import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.utils.Country;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;

public class TestDatasetObjectFactory {

  static final ObjectId id = new ObjectId("67cfeedb4cdf5102acad7395");
  static final ObjectId xsltId = new ObjectId("507f191e810c19729de860ea");
  private static final ZonedDateTime zonedDateTime = ZonedDateTime.parse("2025-03-10T10:10:10.000Z[UTC]");
  static final Date updatedDate = Date.from(zonedDateTime.toInstant());
  static final Date createdDate = Date.from(zonedDateTime.toInstant());

  static DatasetDTO getDatasetDTO() {
    return new DatasetDTO(
        id.toString(),
        "ecloudDatasetId",
        "datasetId",
        "datasetName",
        "organizationId",
        "organizationName",
        "provider",
        "dataProvider",
        "intermediateProvider",
        "createdByUserId",
        "createdByUserName",
        "createdByFirstName",
        "createdByLastName",
        createdDate,
        updatedDate,
        List.of("redirectId1", "redirectId2"),
        "replacedBy",
        "replaces",
        Country.GREECE,
        Language.EL,
        "description",
        PublicationFitness.FIT,
        "notes",
        xsltId
    );
  }

  static Dataset getDataset() {
    Dataset dataset = new Dataset();
    dataset.setId(id);
    dataset.setEcloudDatasetId("ecloudDatasetId");
    dataset.setDatasetId("datasetId");
    dataset.setDatasetName("datasetName");
    dataset.setOrganizationId("organizationId");
    dataset.setOrganizationName("organizationName");
    dataset.setProvider("provider");
    dataset.setDataProvider("dataProvider");
    dataset.setIntermediateProvider("intermediateProvider");
    dataset.setCreatedByUserId("createdByUserId");
    dataset.setCreatedDate(createdDate);
    dataset.setUpdatedDate(updatedDate);
    dataset.setDatasetIdsToRedirectFrom(List.of("redirectId1", "redirectId2"));
    dataset.setReplacedBy("replacedBy");
    dataset.setReplaces("replaces");
    dataset.setCountry(Country.GREECE);
    dataset.setLanguage(Language.EL);
    dataset.setDescription("description");
    dataset.setPublicationFitness(PublicationFitness.FIT);
    dataset.setNotes("notes");
    dataset.setXsltId(xsltId);
    return dataset;
  }
}
