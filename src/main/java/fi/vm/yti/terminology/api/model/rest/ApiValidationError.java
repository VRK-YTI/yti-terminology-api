package fi.vm.yti.terminology.api.model.rest;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiValidationError extends ApiError {

    private List<ApiValidationErrorDetails> details;

    public ApiValidationError(HttpStatus status) {
        super(status);
    }

    public List<ApiValidationErrorDetails> getDetails() {
        return details;
    }

    public void setDetails(List<ApiValidationErrorDetails> details) {
        this.details = details;
    }
}

