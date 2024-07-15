package fi.vm.yti.terminology.api.v2.validator;

import fi.vm.yti.common.validator.BaseValidator;
import fi.vm.yti.common.validator.ValidationConstants;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.stream.Collectors;

public class ConceptValidator extends BaseValidator implements
        ConstraintValidator<ValidConcept, ConceptDTO> {

    boolean update;

    @Override
    public void initialize(ValidConcept constraintAnnotation) {
        this.update = constraintAnnotation.update();
    }

    @Override
    public boolean isValid(ConceptDTO value, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        checkConceptData(context, value);
        checkRecommendedTerms(value.getRecommendedTerms(), context);
        checkTermData(context, value.getRecommendedTerms());
        checkTermData(context, value.getSynonyms());
        checkTermData(context, value.getSearchTerms());
        checkTermData(context, value.getNotRecommendedTerms());
        return !isConstraintViolationAdded();
    }

    private void checkRecommendedTerms(List<TermDTO> terms, ConstraintValidatorContext context) {

        if (terms.isEmpty()) {
            addConstraintViolation(context, "missing-recommended-term", "terms");
        }

        terms.stream()
                .collect(Collectors.groupingBy(TermDTO::getLanguage))
                .forEach((key, value) -> {
                    if (value.size() > 1) {
                        addConstraintViolation(context, "too-many-recommended-terms-" + key, "recommendedTerms");
                    }
                });
    }

    private void checkConceptData(ConstraintValidatorContext context, ConceptDTO dto) {
        if (update && dto.getIdentifier() != null) {
            addConstraintViolation(context, ValidationConstants.MSG_NOT_ALLOWED_UPDATE, "identifier");
        } else {
            checkResourceIdentifier(context, dto.getIdentifier(), update);
        }
        checkCommonTextArea(context, dto.getChangeNote(), "changeNote");
        checkCommonTextArea(context, dto.getHistoryNote(), "historyNote");
        dto.getDefinition().forEach((key, value) -> checkCommonTextArea(context, value, "definition"));
        checkCommonTextField(context, dto.getSubjectArea(), "subjectArea");
        dto.getExamples().forEach(e -> checkCommonTextArea(context, e.getValue(), "examples"));
        dto.getNotes().forEach(e -> checkCommonTextArea(context, e.getValue(), "notes"));
        dto.getEditorialNotes().forEach(e -> checkCommonTextArea(context, e, "editorialNotes"));
        dto.getLinks().forEach(link -> {
            checkRequiredLocalizedValue(context, link.getName(), "links.name");
            checkHasValue(context, link.getUri(), "links.uri");
            link.getDescription().forEach((description, value) -> checkCommonTextArea(context, value, "links.description"));
        });
        checkCommonTextField(context, dto.getConceptClass(), "conceptClass");
        dto.getSources().forEach(s -> checkCommonTextArea(context, s, "sources"));
        checkNotNull(context, dto.getStatus(), "status");
        dto.getReferences().forEach(r -> {
            checkHasValue(context, r.getConceptURI(), "references.conceptURI");
            checkNotNull(context, r.getReferenceType(), "references.referenceType");
        });
    }

    private void checkTermData(ConstraintValidatorContext context, List<TermDTO> terms) {
        terms.forEach(term -> {
            if (!update && term.getIdentifier() != null) {
                addConstraintViolation(context, ValidationConstants.MSG_NOT_ALLOWED_UPDATE, "identifier");
            }
            checkCommonTextArea(context, term.getChangeNote(), "changeNote");
            checkCommonTextArea(context, term.getHistoryNote(), "usageHistory");
            checkCommonTextArea(context, term.getTermInfo(), "termInfo");
            checkHasValue(context, term.getLabel(), "label");
            checkCommonTextField(context, term.getLabel(), "label");
            checkCommonTextArea(context, term.getScope(), "scope");
            checkHasValue(context, term.getLanguage(), "language");
            checkCommonTextField(context, term.getLanguage(), "language");
            checkNotNull(context, term.getStatus(), "status");
            checkCommonTextField(context, term.getTermStyle(), "termStyle");
        });
    }
}
