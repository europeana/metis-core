package eu.europeana.metis.core.rest.config;

import static eu.europeana.metis.core.rest.utils.TestJwtUtils.BEARER;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_INVALID_TOKEN;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_VALID_TOKEN;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_DATASETID;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_XSLT_DEFAULT;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_XSLT_XSLTID;
import static eu.europeana.metis.utils.RestEndpoints.DEPUBLISH_REASONS;
import static java.lang.String.format;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.metis.core.rest.config.TestSecurityConfig.TestController;
import eu.europeana.metis.core.rest.config.properties.SecurityConfigurationProperties;
import eu.europeana.metis.core.rest.utils.TestJwtUtils;
import eu.europeana.metis.core.rest.utils.TestObjectFactory;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(TestController.class)
@ContextConfiguration(classes = {TestController.class, SecurityConfig.class})
class TestSecurityConfig {

  @MockBean
  private JwtDecoder jwtDecoder;

  private static MockMvc mockMvc;
  private final TestJwtUtils testJwtUtils;

  @Autowired
  public TestSecurityConfig(SecurityConfigurationProperties securityConfigurationProperties) {
    testJwtUtils = new TestJwtUtils(securityConfigurationProperties.getResourceNames());
  }

  @BeforeAll
  static void setup(WebApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .apply(SecurityMockMvcConfigurers.springSecurity())
                             .defaultRequest(get("/"))
                             .build();
  }

  @AfterEach
  void cleanUp() {
    reset(jwtDecoder);
  }

  @Test
  void testGetXsltDefault() throws Exception {
    performRequest(() -> get(DATASETS_XSLT_DEFAULT),
        status().isOk(), status().isOk(), status().isOk(), status().isOk());
  }

  @Test
  void testPostXsltDefault() throws Exception {
    performRequest(() -> post(DATASETS_XSLT_DEFAULT),
        status().isOk(), status().isForbidden(), status().isForbidden(), status().isUnauthorized());
  }

  @Test
  void testGetXsltXsltId() throws Exception {
    performRequest(() -> get(DATASETS_XSLT_XSLTID, TestObjectFactory.XSLTID),
        status().isOk(), status().isOk(), status().isOk(), status().isOk());
  }

  @Test
  void testGetDepublishReasons() throws Exception {
    performRequest(() -> get(DEPUBLISH_REASONS, TestObjectFactory.XSLTID),
        status().isOk(), status().isOk(), status().isOk(), status().isOk());
  }

  @Test
  void testGetDataset() throws Exception {
    performRequest(() -> get(DATASETS_DATASETID, TestObjectFactory.DATASETID),
        status().isOk(), status().isOk(), status().isForbidden(), status().isUnauthorized());
  }

  @Test
  void testPostDataset() throws Exception {
    performRequest(() -> post(DATASETS),
        status().isOk(), status().isOk(), status().isForbidden(), status().isUnauthorized());
  }

  private void performRequest(Supplier<MockHttpServletRequestBuilder> requestSupplier,
      ResultMatcher expectedStatusAdmin, ResultMatcher expectedStatusDataOfficer, ResultMatcher expectedStatusOther,
      ResultMatcher expectedStatusUnauthenticated)
      throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getAdminJwt());
    mockMvc.perform(requestSupplier.get().header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(expectedStatusAdmin);

    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(requestSupplier.get().header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(expectedStatusDataOfficer);

    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(requestSupplier.get().header("Authorization", BEARER + MOCK_INVALID_TOKEN))
           .andExpect(expectedStatusOther);

    mockMvc.perform(requestSupplier.get()).andExpect(expectedStatusUnauthenticated);
  }

  /**
   * Fake controller for testing paths from the {@link SecurityConfig}.
   */
  @RestController
  static class TestController {

    @PostMapping(DATASETS_XSLT_DEFAULT)
    public String postDatasetsXsltDefault() {
      return "Success";
    }

    @GetMapping(DATASETS_XSLT_DEFAULT)
    public String getDatasetsXsltDefault() {
      return "Success";
    }

    @GetMapping(DATASETS_XSLT_XSLTID)
    public String datasetsXsltXsltId(@PathVariable("xsltId") String xsltId) {
      return format("Success %s", xsltId);
    }

    @GetMapping(DEPUBLISH_REASONS)
    public String depublishReasons() {
      return "Success";
    }

    @PostMapping(DATASETS)
    public String getPath1() {
      return "Success";
    }

    @GetMapping(DATASETS_DATASETID)
    public String postPath1(@PathVariable("datasetId") String datasetId) {
      return format("Success %s", datasetId);
    }
  }
}
