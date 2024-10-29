package fi.vm.yti.terminology.api.v2.exception;

import fi.vm.yti.common.exception.ApiGenericError;
import fi.vm.yti.common.exception.ExceptionHandlerAdvice;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class TerminologyExceptionHandlerAdvice extends ExceptionHandlerAdvice {

    // Add terminology specific exception handlers

    @ExceptionHandler(ResourceInUseException.class)
    protected ResponseEntity<Object> handleAuthorizationException(ResourceInUseException ex) {
        var apiError = new ApiGenericError(HttpStatus.BAD_REQUEST);
        apiError.setMessage(ex.getMessage());
        apiError.setDetails(ex.getRefList().stream()
                .map(ref -> String.format("%s (%s)", ref.getUri(), ref.getProperty()))
                .collect(Collectors.joining(", ")));
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }

    @ExceptionHandler(ExcelParseException.class)
    protected ResponseEntity<Object> handleExcelImportException(ExcelParseException ex) {
        return new ResponseEntity<>(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ImportException.class)
    protected ResponseEntity<Object> handleGenericImportException(ImportException ex) {
        var apiError = new ApiGenericError(HttpStatus.INTERNAL_SERVER_ERROR);
        apiError.setMessage(ex.getMessage());
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}
