package eu.europeana.metis.core.dataset;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eu.europeana.metis.mongo.utils.ObjectIdSerializer;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * The database structure to hold the dataset identifiers sequence.
 */
@Entity
@Getter
@Setter
public class DatasetIdSequence {

  @Id
  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId id;

  private int sequence;

  public DatasetIdSequence() {
    //Required for json serialization
  }

  /**
   * Initialize a sequence with the provided argument.
   *
   * @param sequence the number to start the sequence from
   */
  public DatasetIdSequence(int sequence) {
    this.sequence = sequence;
  }
}
