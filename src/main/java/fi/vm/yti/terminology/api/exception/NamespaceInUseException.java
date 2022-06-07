package fi.vm.yti.terminology.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class NamespaceInUseException extends RuntimeException {
    private static final long serialVersionUID = 23234342342L;

    public NamespaceInUseException() {
        super("Namespace already in use");
    }
}
