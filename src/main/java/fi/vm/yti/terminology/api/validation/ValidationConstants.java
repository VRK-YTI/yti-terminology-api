package fi.vm.yti.terminology.api.validation;

public class ValidationConstants {

    private ValidationConstants(){
        //Hide constructor as this class has only constants
    }

    public static final String MISSING_VALUE = "Missing value";
    public static final String INVALID_VALUE = "Invalid value";
    public static final String TOO_LONG_VALUE = "Value exceeds character limit";

    public static final int TEXT_FIELD_MAX_LENGTH = 150;
    public static final int TEXT_AREA_MAX_LENGTH = 5000;
    public static final int EMAIL_MAX_LENGTH = 320;

    public static final String PREFIX_REGEX = "[a-z0-9\\-_]*";
}
