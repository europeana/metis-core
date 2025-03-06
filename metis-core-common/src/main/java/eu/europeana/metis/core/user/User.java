package eu.europeana.metis.core.user;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import eu.europeana.metis.mongo.model.HasMongoObjectId;
import eu.europeana.metis.mongo.utils.ObjectIdSerializer;
import java.time.Instant;
import org.bson.types.ObjectId;

/**
 * User model that contains all the required fields for User functionality.
 */
@Entity
@Indexes({
    @Index(fields = {@Field("userId")}, options = @IndexOptions(unique = true))
})
public class User implements HasMongoObjectId {

  @Id
  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId id;

  private String userId;
  private String userName;
  private String firstName;
  private String lastName;
  private Instant expireAt;

  @Override
  public ObjectId getId() {
    return id;
  }

  @Override
  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Instant getExpireAt() {
    return expireAt;
  }

  public void setExpireAt(Instant expireAt) {
    this.expireAt = expireAt;
  }
}
