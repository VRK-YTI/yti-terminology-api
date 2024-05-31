package fi.vm.yti.terminology.api.v2.validator;

import fi.vm.yti.common.validator.BaseValidator;
import fi.vm.yti.common.validator.ValidationConstants;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConceptCollectionValidator extends BaseValidator implements
        ConstraintValidator<ValidConceptCollection, ConceptCollectionDTO> {

    boolean update;

    @Override
    public void initialize(ValidConceptCollection constraintAnnotation) {
        this.update = constraintAnnotation.update();
    }

    @Override
    public boolean isValid(ConceptCollectionDTO value, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        checkConceptCollectionData(context, value);
        return !isConstraintViolationAdded();
    }

    private void checkConceptCollectionData(ConstraintValidatorContext context, ConceptCollectionDTO dto) {
        if (update && dto.getIdentifier() != null) {
            addConstraintViolation(context, ValidationConstants.MSG_NOT_ALLOWED_UPDATE, "identifier");
        } else {
            checkResourceIdentifier(context, dto.getIdentifier(), update);
        }

        dto.getLabel().forEach((key, value) -> checkCommonTextArea(context, value, "label"));
        dto.getDescription().forEach((key, value) -> checkCommonTextArea(context, value, "description"));

        dto.getMembers().forEach(member -> {
            checkHasValue(context, member, "members");
            checkNotNull(context, member, "members");
        });
    }
}
