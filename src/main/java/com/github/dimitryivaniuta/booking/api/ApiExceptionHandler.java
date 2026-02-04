package com.github.dimitryivaniuta.booking.api;

import com.github.dimitryivaniuta.booking.api.dto.ErrorResponse;
import com.github.dimitryivaniuta.booking.config.CorrelationIdFilter;
import com.github.dimitryivaniuta.booking.service.BadRequestException;
import com.github.dimitryivaniuta.booking.service.ConflictException;
import com.github.dimitryivaniuta.booking.service.NotFoundException;
import com.github.dimitryivaniuta.booking.service.ForbiddenException;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions into API responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ErrorResponse notFound(NotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  public ErrorResponse conflict(ConflictException ex) {
    return error(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  public ErrorResponse badRequest(BadRequestException ex) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ErrorResponse forbidden(ForbiddenException ex) {
    return error(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ErrorResponse validation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getAllErrors().stream()
        .findFirst()
        .map(e -> e.getDefaultMessage())
        .orElse("Validation error.");
    return error(HttpStatus.BAD_REQUEST, msg);
  }

  @ExceptionHandler(Exception.class)
  public ErrorResponse generic(Exception ex) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error.");
  }

  private ErrorResponse error(HttpStatus status, String msg) {
    String corrId = MDC.get(CorrelationIdFilter.MDC_KEY);
    return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), msg, corrId);
  }
}
