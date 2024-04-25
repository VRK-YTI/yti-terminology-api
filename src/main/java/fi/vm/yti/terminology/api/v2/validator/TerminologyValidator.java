package fi.vm.yti.terminology.api.v2.validator;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.dto.MetaDataDTO;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.validator.MetaDataValidator;
import fi.vm.yti.common.validator.ValidationConstants;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class TerminologyValidator extends MetaDataValidator<TerminologyRepository> implements
        ConstraintValidator<ValidTerminology, MetaDataDTO> {

    boolean update;

    public TerminologyValidator(TerminologyRepository repository) {
        super(repository);
    }

    @Override
    public void initialize(ValidTerminology constraintAnnotation) {
        update = constraintAnnotation.update();
    }

    @Override
    public boolean isValid(MetaDataDTO value, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkModelPrefix(context, value, update);

        checkLanguages(context, value);
        checkLabels(context, value);
        checkDescription(context, value);
        checkOrganizations(context, value);
        checkGroups(context, value);
        checkNotNull(context, value.getStatus(), "status");
        checkGraphType(context, value);

        return !isConstraintViolationAdded();
    }


    private void checkGraphType(ConstraintValidatorContext context, MetaDataDTO value) {
        if (value.getGraphType() != null
            && !Arrays.asList(GraphType.TERMINOLOGICAL_VOCABULARY, GraphType.OTHER_VOCABULARY).contains(value.getGraphType())) {
                addConstraintViolation(context, ValidationConstants.MSG_VALUE_INVALID, "graphType");
        } else if (value.getGraphType() == null) {
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "graphType");
        }
    }

    @Override
    public String getNamespacePrefix() {
        return Constants.TERMINOLOGY_NAMESPACE;
    }
}
