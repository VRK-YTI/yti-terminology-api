package fi.vm.yti.terminology.api.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VocabularyNodeValidator.class)
public @interface ValidVocabularyNode {

    String message() default "Invalid data";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
