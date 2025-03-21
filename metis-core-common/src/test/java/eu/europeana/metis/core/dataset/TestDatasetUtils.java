package eu.europeana.metis.core.dataset;

import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.utils.Country;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;

public class TestDatasetUtils {

  private static final ZonedDateTime zonedDateTime = ZonedDateTime.parse("2025-03-10T10:10:10.000Z[UTC]");
  //FIELDS
  static final String ID = "id";
  static final String ECLOUD_DATASET_ID = "ecloudDatasetId";
  static final String DATASET_ID = "datasetId";
  static final String DATASET_NAME = "datasetName";
  static final String PROVIDER = "provider";
  static final String DATA_PROVIDER = "dataProvider";
  static final String INTERMEDIATE_PROVIDER = "intermediateProvider";
  static final String CREATED_BY_USER_ID = "createdByUserId";
  static final String CREATED_BY_USER_NAME = "createdByUserName";
  static final String CREATED_BY_FIRST_NAME = "createdByFirstName";
  static final String CREATED_BY_LAST_NAME = "createdByLastName";
  static final String CREATED_DATE = "createdDate";
  static final String UPDATED_DATE = "updatedDate";
  static final String DATASET_IDS_TO_REDIRECT_FROM = "datasetIdsToRedirectFrom";
  static final String REPLACED_BY = "replacedBy";
  static final String REPLACES = "replaces";
  static final String COUNTRY = "country";
  static final String LANGUAGE = "language";
  static final String COUNTRY_ENUM = "enum";
  static final String LANGUAGE_ENUM = "enum";
  static final String DESCRIPTION = "description";
  static final String PUBLICATION_FITNESS = "publicationFitness";
  static final String NOTES = "notes";
  static final String XSLT_ID = "xsltId";
  //VALUES
  static final ObjectId OBJECT_ID_VALUE = new ObjectId("67cfeedb4cdf5102acad7395");
  static final ObjectId XSLT_OBJECT_ID_VALUE = new ObjectId("507f191e810c19729de860ea");
  static final Date CREATED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  static final Date UPDATED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  static final String REDIRECT_ID_1_VALUE = "redirectId1";
  static final String REDIRECT_ID_2_VALUE = "redirectId2";

  public static DatasetDTO getDatasetDTO() {
    return new DatasetDTO(
        OBJECT_ID_VALUE.toString(),
        ECLOUD_DATASET_ID,
        DATASET_ID,
        DATASET_NAME,
        PROVIDER,
        DATA_PROVIDER,
        INTERMEDIATE_PROVIDER,
        CREATED_BY_USER_ID,
        CREATED_BY_USER_NAME,
        CREATED_BY_FIRST_NAME,
        CREATED_BY_LAST_NAME,
        CREATED_DATE_VALUE,
        UPDATED_DATE_VALUE,
        List.of(REDIRECT_ID_1_VALUE, REDIRECT_ID_2_VALUE),
        REPLACED_BY,
        REPLACES,
        Country.GREECE,
        Language.EL,
        DESCRIPTION,
        PublicationFitness.FIT,
        NOTES,
        XSLT_OBJECT_ID_VALUE.toString()
    );
  }

  static DatasetDTO getDatasetDTOUsingSetters() {
    DatasetDTO datasetDTO = getDatasetDTO();
    DatasetDTO datasetDTO1 = new DatasetDTO();
    datasetDTO1.setId(datasetDTO.getId());
    datasetDTO1.setEcloudDatasetId(datasetDTO.getEcloudDatasetId());
    datasetDTO1.setDatasetId(datasetDTO.getDatasetId());
    datasetDTO1.setDatasetName(datasetDTO.getDatasetName());
    datasetDTO1.setProvider(datasetDTO.getProvider());
    datasetDTO1.setDataProvider(datasetDTO.getDataProvider());
    datasetDTO1.setIntermediateProvider(datasetDTO.getIntermediateProvider());
    datasetDTO1.setCreatedByUserId(datasetDTO.getCreatedByUserId());
    datasetDTO1.setCreatedByUserName(datasetDTO.getCreatedByUserName());
    datasetDTO1.setCreatedByFirstName(datasetDTO.getCreatedByFirstName());
    datasetDTO1.setCreatedByLastName(datasetDTO.getCreatedByLastName());
    datasetDTO1.setCreatedDate(datasetDTO.getCreatedDate());
    datasetDTO1.setUpdatedDate(datasetDTO.getUpdatedDate());
    datasetDTO1.setDatasetIdsToRedirectFrom(datasetDTO.getDatasetIdsToRedirectFrom());
    datasetDTO1.setReplacedBy(datasetDTO.getReplacedBy());
    datasetDTO1.setReplaces(datasetDTO.getReplaces());
    datasetDTO1.setCountry(datasetDTO.getCountry());
    datasetDTO1.setLanguage(datasetDTO.getLanguage());
    datasetDTO1.setDescription(datasetDTO.getDescription());
    datasetDTO1.setPublicationFitness(datasetDTO.getPublicationFitness());
    datasetDTO1.setNotes(datasetDTO.getNotes());
    datasetDTO1.setXsltId(datasetDTO.getXsltId());
    return datasetDTO1;
  }

  static DatasetDTO getDatasetDTOWithNullValues() {
    DatasetDTO datasetDTO = getDatasetDTO();
    return new DatasetDTO(
        datasetDTO.getId(),
        datasetDTO.getEcloudDatasetId(),
        datasetDTO.getDatasetId(),
        datasetDTO.getDatasetName(),
        datasetDTO.getProvider(),
        datasetDTO.getDataProvider(),
        datasetDTO.getIntermediateProvider(),
        datasetDTO.getCreatedByUserId(),
        datasetDTO.getCreatedByUserName(),
        datasetDTO.getCreatedByFirstName(),
        datasetDTO.getCreatedByLastName(),
        null,
        null,
        null,
        datasetDTO.getReplacedBy(),
        datasetDTO.getReplaces(),
        datasetDTO.getCountry(),
        datasetDTO.getLanguage(),
        datasetDTO.getDescription(),
        datasetDTO.getPublicationFitness(),
        datasetDTO.getNotes(),
        datasetDTO.getXsltId());
  }

  static DatasetDTO getDatasetDTOUsingSettersWithNullValues() {
    DatasetDTO datasetDTO = getDatasetDTO();
    datasetDTO.setCreatedDate(null);
    datasetDTO.setUpdatedDate(null);
    datasetDTO.setDatasetIdsToRedirectFrom(null);
    return datasetDTO;
  }

  public static Dataset getDataset() {
    Dataset dataset = new Dataset();
    dataset.setId(OBJECT_ID_VALUE);
    dataset.setEcloudDatasetId(ECLOUD_DATASET_ID);
    dataset.setDatasetId(DATASET_ID);
    dataset.setDatasetName(DATASET_NAME);
    dataset.setProvider(PROVIDER);
    dataset.setDataProvider(DATA_PROVIDER);
    dataset.setIntermediateProvider(INTERMEDIATE_PROVIDER);
    dataset.setCreatedByUserId(CREATED_BY_USER_ID);
    dataset.setCreatedDate(CREATED_DATE_VALUE);
    dataset.setUpdatedDate(UPDATED_DATE_VALUE);
    dataset.setDatasetIdsToRedirectFrom(List.of(REDIRECT_ID_1_VALUE, REDIRECT_ID_2_VALUE));
    dataset.setReplacedBy(REPLACED_BY);
    dataset.setReplaces(REPLACES);
    dataset.setCountry(Country.GREECE);
    dataset.setLanguage(Language.EL);
    dataset.setDescription(DESCRIPTION);
    dataset.setPublicationFitness(PublicationFitness.FIT);
    dataset.setNotes(NOTES);
    dataset.setXsltId(XSLT_OBJECT_ID_VALUE);
    return dataset;
  }

  static Dataset getDatasetUsingSettersWithNullValues() {
    Dataset dataset = getDataset();
    dataset.setCreatedDate(null);
    dataset.setUpdatedDate(null);
    dataset.setDatasetIdsToRedirectFrom(null);
    return dataset;
  }
}
