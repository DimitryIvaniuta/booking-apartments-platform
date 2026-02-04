package com.github.dimitryivaniuta.booking.auth.api;

import com.github.dimitryivaniuta.booking.api.dto.ErrorResponse;
import com.github.dimitryivaniuta.booking.config.CorrelationIdFilter;
import com.github.dimitryivaniuta.booking.auth.service.AuthBadRequestException;
import com.github.dimitryivaniuta.booking.auth.service.AuthForbiddenException;
import com.github.dimitryivaniuta.booking.auth.service.AuthLockedException;
import com.github.dimitryivaniuta.booking.auth.service.AuthNotFoundException;
import com.github.dimitryivaniuta.booking.auth.service.AuthUnauthorizedException;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps auth domain exceptions to API error responses.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

  @ExceptionHandler(AuthBadRequestException.class)
  public ErrorResponse badRequest(AuthBadRequestException ex) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(AuthUnauthorizedException.class)
  public ErrorResponse unauthorized(AuthUnauthorizedException ex) {
    return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(AuthForbiddenException.class)
  public ErrorResponse forbidden(AuthForbiddenException ex) {
    return error(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(AuthLockedException.class)
  public ErrorResponse locked(AuthLockedException ex) {
    return error(HttpStatus.LOCKED, ex.getMessage());
  }

  @ExceptionHandler(AuthNotFoundException.class)
  public ErrorResponse notFound(AuthNotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  private ErrorResponse error(HttpStatus status, String msg) {
    String corrId = MDC.get(CorrelationIdFilter.MDC_KEY);
    return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), msg, corrId);
  }
}
