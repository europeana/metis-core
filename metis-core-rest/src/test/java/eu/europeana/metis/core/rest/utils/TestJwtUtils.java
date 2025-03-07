package eu.europeana.metis.core.rest.utils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.oauth2.jwt.Jwt;

import static eu.europeana.metis.core.common.AccountRole.ADMIN;
import static eu.europeana.metis.core.common.AccountRole.DATA_OFFICER;

public class TestJwtUtils {

  public static final String BEARER = "Bearer ";
  public static final String MOCK_VALID_TOKEN = "xxx.yyy.zzz";
  public static final String MOCK_INVALID_TOKEN = "invalidToken";
  private final List<String> resourceNames;

  public TestJwtUtils(List<String> resourceNames) {
    this.resourceNames = resourceNames;
  }

  public Jwt getAdminJwt() {
    return getJwt(MOCK_VALID_TOKEN, List.of(ADMIN.toString()));
  }

  public Jwt getDataOfficerJwt() {
    return getJwt(MOCK_VALID_TOKEN, List.of(DATA_OFFICER.toString()));
  }

  public Jwt getInvalidRoleJwt() {
    return getJwt(MOCK_INVALID_TOKEN, List.of("INVALID"));
  }

  private @NotNull Jwt getJwt(String token, List<String> resourceAccessRoles) {
    Map<String, Object> resourceAccess =
        resourceNames.stream().collect(Collectors.toMap(name -> name,
                         name -> Map.of("roles", resourceAccessRoles)));

    return Jwt.withTokenValue(token)
              .header("alg", "none")
              .claim("resource_access", resourceAccess)
              .claim("email", "user@example.com")
              .claim("sub", UUID.randomUUID().toString())
              .claim("preferred_username", "userName")
              .claim("given_name", "firstName")
              .claim("family_name", "lastName")
              .claim("family_name", "lastName")
              .claim("iat", Instant.now())
              .build();
  }

}
