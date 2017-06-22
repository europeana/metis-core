package eu.europeana.metis.core.rest.exception;

import eu.europeana.metis.core.exceptions.ApiKeyNotAuthorizedException;
import eu.europeana.metis.core.exceptions.BadContentException;
import eu.europeana.metis.core.exceptions.DatasetAlreadyExistsException;
import eu.europeana.metis.core.exceptions.EmptyApiKeyException;
import eu.europeana.metis.core.exceptions.NoApiKeyFoundException;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoOrganizationFoundException;
import eu.europeana.metis.core.exceptions.NoUserWorkflowExecutionFoundException;
import eu.europeana.metis.core.exceptions.NoUserWorkflowFoundException;
import eu.europeana.metis.core.exceptions.OrganizationAlreadyExistsException;
import eu.europeana.metis.core.exceptions.StructuredExceptionWrapper;
import eu.europeana.metis.core.exceptions.UserNotFoundException;
import eu.europeana.metis.core.exceptions.UserWorkflowExecutionAlreadyExistsException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-05-10
 */
@ControllerAdvice
public class RestResponseExceptionHandler {

  @ExceptionHandler(value = {UserNotFoundException.class, ApiKeyNotAuthorizedException.class,
      NoApiKeyFoundException.class, IOException.class,
      SolrServerException.class, OrganizationAlreadyExistsException.class, ServletException.class,
      NoOrganizationFoundException.class, BadContentException.class,
      DatasetAlreadyExistsException.class,
      NoDatasetFoundException.class, NoUserWorkflowFoundException.class,
      UserWorkflowExecutionAlreadyExistsException.class,
      NoUserWorkflowExecutionFoundException.class, ExecutionException.class,
      InterruptedException.class, EmptyApiKeyException.class})
  @ResponseBody
  public StructuredExceptionWrapper handleException(HttpServletRequest request, Exception ex,
      HttpServletResponse response) {
    HttpStatus status = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class).value();
    response.setStatus(status.value());
    return new StructuredExceptionWrapper(ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseBody
  public StructuredExceptionWrapper handleMessageNotReadable(HttpMessageNotReadableException ex,
      HttpServletResponse response) {
    response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
    return new StructuredExceptionWrapper(
        "Message body not readable. It is missing or malformed\n" + ex.getMessage());
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseBody
  public StructuredExceptionWrapper handleMissingParams(MissingServletRequestParameterException ex,
      HttpServletResponse response) {
    response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
    return new StructuredExceptionWrapper(ex.getParameterName() + " parameter is missing");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseBody
  public StructuredExceptionWrapper handleMissingParams(HttpRequestMethodNotSupportedException ex,
      HttpServletResponse response) {
    response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
    return new StructuredExceptionWrapper("Method not allowed: " + ex.getMessage());
  }


  @ExceptionHandler(value = {IllegalStateException.class, MethodArgumentTypeMismatchException.class})
  @ResponseBody
  public StructuredExceptionWrapper handleMessageNotReadable(Exception ex,
      HttpServletResponse response) {
    response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
    return new StructuredExceptionWrapper(
        "Request not readable.\n" + ex.getMessage());
  }
}