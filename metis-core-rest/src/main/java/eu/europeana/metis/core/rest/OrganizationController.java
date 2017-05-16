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
package eu.europeana.metis.core.rest;

import static eu.europeana.metis.RestEndpoints.CRM_ORGANIZATIONS;
import static eu.europeana.metis.RestEndpoints.CRM_ORGANIZATION_ID;
import static eu.europeana.metis.RestEndpoints.ORGANIZATIONS_BYDATASET;
import static eu.europeana.metis.RestEndpoints.ORGANIZATION_OPTED_IN;

import eu.europeana.metis.RestEndpoints;
import eu.europeana.metis.core.api.MetisKey;
import eu.europeana.metis.core.api.Options;
import eu.europeana.metis.core.api.Profile;
import eu.europeana.metis.core.common.Country;
import eu.europeana.metis.core.common.OrganizationRole;
import eu.europeana.metis.core.dataset.DatasetListWrapper;
import eu.europeana.metis.core.exceptions.ApiKeyNotAuthorizedException;
import eu.europeana.metis.core.exceptions.BadContentException;
import eu.europeana.metis.core.exceptions.NoApiKeyFoundException;
import eu.europeana.metis.core.exceptions.NoOrganizationFoundException;
import eu.europeana.metis.core.exceptions.OrganizationAlreadyExistsException;
import eu.europeana.metis.core.organization.Organization;
import eu.europeana.metis.core.organization.OrganizationListWrapper;
import eu.europeana.metis.core.rest.response.MetisOrganizationView;
import eu.europeana.metis.core.rest.response.PublicOrganizationView;
import eu.europeana.metis.core.rest.utils.JsonUtils;
import eu.europeana.metis.core.search.common.OrganizationSearchBean;
import eu.europeana.metis.core.search.common.OrganizationSearchListWrapper;
import eu.europeana.metis.core.service.DatasetService;
import eu.europeana.metis.core.service.MetisAuthorizationService;
import eu.europeana.metis.core.service.OrganizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * The organization controller
 * Created by ymamakis on 2/18/16.
 */
@Controller
@Api("/")
public class OrganizationController {

  private final Logger LOGGER = LoggerFactory.getLogger(OrganizationController.class);

  @Autowired
  private OrganizationService organizationService;
  @Autowired
  private DatasetService datasetService;
  @Autowired
  private MetisAuthorizationService authorizationService;

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS, method = RequestMethod.POST, consumes = {
      MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Successful response"),
      @ApiResponse(code = 401, message = "Api Key not authorized"),
      @ApiResponse(code = 406, message = "Bad content"),
      @ApiResponse(code = 409, message = "Organization already exists")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true)
  })
  @ApiOperation(value = "Create an organization")
  public void createOrganization(@RequestBody Organization organization,
      @QueryParam("apikey") String apikey)
      throws IOException, SolrServerException, ApiKeyNotAuthorizedException, NoApiKeyFoundException, OrganizationAlreadyExistsException, BadContentException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null) {
      if (key.getOptions().equals(Options.WRITE)) {
        try {
          Organization storedOrganization = organizationService
              .getOrganizationByOrganizationId(organization.getOrganizationId());
          if (storedOrganization != null) {
            throw new OrganizationAlreadyExistsException(organization.getOrganizationId());
          }
        } catch (NoOrganizationFoundException e) {
          LOGGER.info("Organization not found, so can be created");
        }

        if (StringUtils.isEmpty(organization.getOrganizationId())) {
          throw new BadContentException("OrganizationId cannot be null");
        }

        organizationService.createOrganization(organization);
        LOGGER.info("Organization with id " + organization.getOrganizationId() + " created");
      } else {
        throw new ApiKeyNotAuthorizedException(apikey);
      }
    } else {
      throw new NoApiKeyFoundException(apikey);
    }
  }

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS_ORGANIZATION_ID, method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Successful response"),
      @ApiResponse(code = 401, message = "Api Key not authorized")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true),
      @ApiImplicitParam(name = "organizationId", value = "OrganizationId", dataType = "string", paramType = "path", required = true)
  })
  @ApiOperation(value = "Delete an organization by organization Id")
  public void deleteOrganization(@PathVariable("organizationId"
  ) String organizationId, @QueryParam("apikey") String apikey)
      throws IOException, SolrServerException, ApiKeyNotAuthorizedException, NoApiKeyFoundException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null) {
      if (key.getOptions().equals(Options.WRITE)) {
        organizationService.deleteOrganizationByOrganizationId(organizationId);
        LOGGER.info("Organization with id " + organizationId + " deleted");
      } else {
        throw new ApiKeyNotAuthorizedException(apikey);
      }
    } else {
      throw new NoApiKeyFoundException(apikey);
    }
  }

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS_ORGANIZATION_ID, method = RequestMethod.PUT, consumes = {
      MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Successful response"),
      @ApiResponse(code = 404, message = "Organization not found"),
      @ApiResponse(code = 406, message = "Bad content")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true),
      @ApiImplicitParam(name = "organizationId", value = "OrganizationId", dataType = "string", paramType = "path", required = true)
  })
  @ApiOperation(value = "Update an organization by organization Id")
  public void updateOrganization(@RequestBody Organization organization,
      @PathVariable("organizationId"
      ) String organizationId, @QueryParam("apikey") String apikey)
      throws ApiKeyNotAuthorizedException, NoApiKeyFoundException, IOException, SolrServerException, NoOrganizationFoundException, BadContentException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null) {
      if (key.getOptions().equals(Options.WRITE)) {
        if (!StringUtils.isEmpty(organization.getOrganizationId()) && !organization
            .getOrganizationId().equals(organizationId)) {
          throw new BadContentException(
              "OrganinazationId in body " + organization.getOrganizationId()
                  + " is different from parameter " + organizationId);
        }
        organization.setOrganizationId(organizationId);

        //Check if exists first
        organizationService.getOrganizationByOrganizationId(organization.getOrganizationId());
        organizationService.updateOrganization(organization);
        LOGGER.info("Organization with id " + organizationId + " updated");
      } else {
        throw new ApiKeyNotAuthorizedException(apikey);
      }
    } else {
      throw new NoApiKeyFoundException(apikey);
    }
  }

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful response"),
      @ApiResponse(code = 401, message = "Api Key not authorized")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true),
      @ApiImplicitParam(name = "nextPage", value = "nextPage", dataType = "string", paramType = "query")
  })
  @ApiOperation(value = "Get all organizations")
  public OrganizationListWrapper getAllOrganizations(@QueryParam("nextPage"
  ) String nextPage, @QueryParam("apikey") String apikey)
      throws IllegalAccessException, InstantiationException, NoApiKeyFoundException, ApiKeyNotAuthorizedException, NoOrganizationFoundException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null && (key.getOptions().equals(Options.WRITE) || key.getOptions()
        .equals(Options.READ))) {
      List<Organization> organizations = organizationService.getAllOrganizations(nextPage);
      OrganizationListWrapper organizationListWrapper = new OrganizationListWrapper();
      organizationListWrapper.setOrganizationsAndLastPage(organizations,
          organizationService.getOrganizationsPerRequestLimit());
      LOGGER.info("Batch of: " + organizationListWrapper.getListSize()
          + " organizations returned, using batch nextPage: " + nextPage);
      return organizationListWrapper;
    } else if (key == null) {
      throw new NoApiKeyFoundException(apikey);
    } else {
      throw new ApiKeyNotAuthorizedException(apikey);
    }
  }

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS_ORGANIZATION_ID, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful response"),
      @ApiResponse(code = 401, message = "Api Key not authorized"),
      @ApiResponse(code = 404, message = "Organization not found")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true),
      @ApiImplicitParam(name = "organizationId", value = "OrganizationId", dataType = "string", paramType = "path", required = true)
  })
  @ApiOperation(value = "Get an organization by organization Id", response = Organization.class)
  public Organization getOrganizationByOrganizationId(
      @PathVariable("organizationId") String organizationId, @QueryParam("apikey") String apikey)
      throws NoApiKeyFoundException, ApiKeyNotAuthorizedException, NoOrganizationFoundException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null && (key.getOptions().equals(Options.WRITE) || key.getOptions()
        .equals(Options.READ))) {
      Organization organization = organizationService
          .getOrganizationByOrganizationId(organizationId);
      LOGGER.info("Organization with id " + organizationId + " found");
      return organization;
    } else if (key == null) {
      throw new NoApiKeyFoundException(apikey);
    } else {
      throw new ApiKeyNotAuthorizedException(apikey);
    }
  }

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS_COUNTRY_ISOCODE, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful response"),
      @ApiResponse(code = 401, message = "Api Key not authorized")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true),
      @ApiImplicitParam(name = "nextPage", value = "nextPage", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "isoCode", value = "IsoCode", dataType = "string", paramType = "path", required = true)
  })
  @ApiOperation(value = "Get all organizations by county isoCode", response = OrganizationListWrapper.class)
  public OrganizationListWrapper getAllOrganizationsByCountryIsoCode(
      @PathVariable("isoCode") String isoCode, @QueryParam("nextPage"
  ) String nextPage, @QueryParam("apikey") String apikey)
      throws NoOrganizationFoundException, NoApiKeyFoundException, ApiKeyNotAuthorizedException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null && (key.getOptions().equals(Options.WRITE) || key.getOptions()
        .equals(Options.READ))) {
      List<Organization> organizations = organizationService
          .getAllOrganizationsByCountry(Country.toCountry(isoCode), nextPage);
      OrganizationListWrapper organizationListWrapper = new OrganizationListWrapper();
      organizationListWrapper.setOrganizationsAndLastPage(organizations,
          organizationService.getOrganizationsPerRequestLimit());
      LOGGER.info("Batch of: " + organizationListWrapper.getListSize()
          + " organizations returned, using batch nextPage: " + nextPage);
      return organizationListWrapper;
    } else if (key == null) {
      throw new NoApiKeyFoundException(apikey);
    } else {
      throw new ApiKeyNotAuthorizedException(apikey);
    }
  }

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS_ROLES, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful response"),
      @ApiResponse(code = 401, message = "Api Key not authorized"),
      @ApiResponse(code = 406, message = "Bad content")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true),
      @ApiImplicitParam(name = "nextPage", value = "nextPage", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "organizationRoles", value = "comma separated values, e.g. CONTENT_PROVIDER,EUROPEANA", allowMultiple = true, dataType = "string", paramType = "query", required = true)
  })
  @ApiOperation(value = "Get all organizations by organization roles", response = OrganizationListWrapper.class)
  public OrganizationListWrapper getAllOrganizationsByOrganizationRoles(
      @RequestParam("organizationRoles") List<OrganizationRole> organizationRoles,
      @QueryParam("nextPage") String nextPage,
      @QueryParam("apikey") String apikey)
      throws BadContentException, NoApiKeyFoundException, ApiKeyNotAuthorizedException, NoOrganizationFoundException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null && (key.getOptions().equals(Options.WRITE) || key.getOptions()
        .equals(Options.READ))) {
      if (organizationRoles != null) {
        List<Organization> organizations = organizationService
            .getAllOrganizationsByOrganizationRole(organizationRoles, nextPage);
        OrganizationListWrapper organizationListWrapper = new OrganizationListWrapper();
        organizationListWrapper.setOrganizationsAndLastPage(organizations,
            organizationService.getOrganizationsPerRequestLimit());
        LOGGER.info("Batch of: " + organizationListWrapper.getListSize()
            + " organizations returned, using batch nextPage: " + nextPage);
        return organizationListWrapper;
      } else {
        throw new BadContentException("Organization roles malformed or empty");
      }
    } else if (key == null) {
      throw new NoApiKeyFoundException(apikey);
    } else {
      throw new ApiKeyNotAuthorizedException(apikey);
    }
  }

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS_SUGGEST, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful response"),
      @ApiResponse(code = 401, message = "Api Key not authorized")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true),
      @ApiImplicitParam(name = "searchTerm", value = "search value to get suggestions from", dataType = "string", paramType = "query")
  })
  @ApiOperation(value = "Suggest Organizations by a search term")
  public OrganizationSearchListWrapper suggestOrganizations(
      @QueryParam("searchTerm") String searchTerm,
      @QueryParam("apikey") String apikey)
      throws IOException, SolrServerException, NoApiKeyFoundException, ApiKeyNotAuthorizedException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null && (key.getOptions().equals(Options.WRITE) || key.getOptions()
        .equals(Options.READ))) {
      List<OrganizationSearchBean> organizationSearchBeans = organizationService
          .suggestOrganizations(searchTerm);
      return new OrganizationSearchListWrapper(organizationSearchBeans);
    } else if (key == null) {
      throw new NoApiKeyFoundException(apikey);
    } else {
      throw new ApiKeyNotAuthorizedException(apikey);
    }
  }

  @RequestMapping(value = RestEndpoints.ORGANIZATIONS_ORGANIZATION_ID_DATASETS, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful response"),
      @ApiResponse(code = 401, message = "Api Key not authorized"),
      @ApiResponse(code = 404, message = "Organization not found")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = "apikey", value = "ApiKey", dataType = "string", paramType = "query", required = true),
      @ApiImplicitParam(name = "nextPage", value = "nextPage", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "organizationId", value = "OrganizationId", dataType = "string", paramType = "path", required = true)
  })
  @ApiOperation(value = "Get all the dataset by organization Id", response = DatasetListWrapper.class)
  public DatasetListWrapper getAllDatasetsByOrganizationId(
      @PathVariable("organizationId") String organizationId,
      @QueryParam("nextPage") String nextPage, @QueryParam("apikey") String apikey)
      throws NoApiKeyFoundException, ApiKeyNotAuthorizedException, NoOrganizationFoundException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null && (key.getOptions().equals(Options.WRITE) || key.getOptions()
        .equals(Options.READ))) {
      DatasetListWrapper datasetListWrapper = new DatasetListWrapper();
      datasetListWrapper.setDatasetsAndLastPage(
          organizationService.getAllDatasetsByOrganizationId(organizationId, nextPage),
          datasetService.getDatasetsPerRequestLimit());
      return datasetListWrapper;
    } else if (key == null) {
      throw new NoApiKeyFoundException(apikey);
    } else {
      throw new ApiKeyNotAuthorizedException(apikey);
    }
  }

  /**
   * Retrieve the organization with a specific organization from CRM
   *
   * @param id The organization id of the organization to retrieve
   * @return The organization with the specified id
   */
  @RequestMapping(value = CRM_ORGANIZATION_ID, method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  @ApiOperation(value = "Retrieve an organization from CRM", response = Organization.class)
  public ModelAndView getOrganizationByOrganizationIdFromCRM(
      @ApiParam("orgId") @PathVariable(value = "orgId") String id,
      @RequestParam("apikey") String apikey)
      throws
      NoOrganizationFoundException, IOException, ParseException, InstantiationException, IllegalAccessException, NoApiKeyFoundException {
    Organization org = organizationService.getOrganizationByIdFromCRM(id);
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null) {
      if (key.getProfile().equals(Profile.PUBLIC)) {
        return PublicOrganizationView.generateResponse(org);
      } else {
        return MetisOrganizationView.generateResponse(org);
      }
    }
    throw new NoApiKeyFoundException(apikey);
  }

  /**
   * Retrieve all the organizations from CRM
   *
   * @return The organization with the specified id
   */
  @RequestMapping(value = CRM_ORGANIZATIONS, method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  @ApiOperation(value = "Retrieve all the organizations from CRM", response = OrganizationListWrapper.class)
  public ModelAndView getOrganizationsFromCRM(@RequestParam("apikey") String apikey)
      throws
      NoOrganizationFoundException, IOException, ParseException, IllegalAccessException, InstantiationException, NoApiKeyFoundException {
    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null) {
      return constructModelAndViewForList(key, organizationService.getAllOrganizationsFromCRM());
    }
    throw new NoApiKeyFoundException(apikey);
  }

  /**
   * Check whether an organization is opted in for using the Image Service of Europeana or not
   *
   * @param id The id of the organization
   * @return true if opted in, false otherwise
   */
  @RequestMapping(value = ORGANIZATION_OPTED_IN, method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  @ApiOperation(value = "Check whether an organization is opted-in for IIIF or not")
  public ModelAndView isOptedIn(@PathVariable("id") String id) {
    ModelAndView view = new ModelAndView("json");
    view.addObject("result", organizationService.isOptedInForIIIF(id));
    return view;
  }

  /**
   * Retrieve organizations by dataset and data provider id
   *
   * @param datasetId The dataset Id
   * @param dataproviderId The data provider id
   * @param apikey The API key
   * @return The lsit of organizatios fro a given dataset
   */
  @RequestMapping(value = ORGANIZATIONS_BYDATASET, method = RequestMethod.GET, produces = "application/json")
  @ResponseBody
  @ApiOperation(value = "Get organizations that refer to a dataset as provider")
  public ModelAndView getOrganizationsByDatasetId(@PathVariable("id") String datasetId,
      @RequestParam("dataProviderId") String dataproviderId,
      @RequestParam("apikey") String apikey)
      throws NoApiKeyFoundException, IllegalAccessException, InstantiationException {

    MetisKey key = authorizationService.getKeyFromId(apikey);
    if (key != null) {
      List<Organization> orgs = organizationService.getByDatasetId(datasetId, dataproviderId);
      return constructModelAndViewForList(key, orgs);
    }
    throw new NoApiKeyFoundException(apikey);
  }

  private ModelAndView constructModelAndViewForList(MetisKey key, List<Organization> orgs)
      throws InstantiationException, IllegalAccessException {
    if (key.getProfile().equals(Profile.PUBLIC)) {
      List<ModelAndView> organizationViews = new ArrayList<>();
      for (Organization org : orgs) {
        organizationViews.add(PublicOrganizationView.generateResponse(org));
      }
      return JsonUtils.toJson(organizationViews);
    } else {
      List<ModelAndView> organizationViews = new ArrayList<>();
      for (Organization org : orgs) {
        organizationViews.add(MetisOrganizationView.generateResponse(org));
      }
      return JsonUtils.toJson(organizationViews);
    }
  }

}
