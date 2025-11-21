package eu.europeana.metis.core.rest.exception;

import jakarta.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Class adds global logging stack-traces of exceptions which were thrown by resource controller and
 * were automatically converted to http responses by RestResponseExceptionHandler.
 */
@Order(0) // high precedence
@ControllerAdvice
public class LoggingControllerAdvice {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
    @ExceptionHandler(Exception.class)
    public void logException(Exception ex, HttpServletResponse response) {
      LOGGER.warn("Exception during REST request execution!", ex);
    }
}