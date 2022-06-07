package fi.vm.yti.terminology.api.model.rest;

public class ApiValidationErrorDetails {

    private String field;

    private Object rejectedValue;

    private String message;

    public ApiValidationErrorDetails(String message, String field, String rejectedValue) {
        this.message = message;
        this.rejectedValue = rejectedValue;
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public String getMessage() {
        return message;
    }
}

