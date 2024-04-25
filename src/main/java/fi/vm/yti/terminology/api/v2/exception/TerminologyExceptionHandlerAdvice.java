package fi.vm.yti.terminology.api.v2.exception;

import fi.vm.yti.common.exception.ExceptionHandlerAdvice;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class TerminologyExceptionHandlerAdvice extends ExceptionHandlerAdvice {

    // Add terminology specific exception handlers
}
