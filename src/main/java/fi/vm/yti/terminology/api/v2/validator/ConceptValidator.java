package fi.vm.yti.terminology.api.v2.validator;

import fi.vm.yti.common.validator.BaseValidator;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.enums.TermType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashMap;
import java.util.Set;

public class ConceptValidator extends BaseValidator implements
        ConstraintValidator<ValidConcept, ConceptDTO> {
    @Override
    public void initialize(ValidConcept constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(ConceptDTO value, ConstraintValidatorContext context) {
        checkConceptData(context, value);

        var handledLangs = new HashMap<String, String>();
        value.getTerms().stream()
                .filter(t -> t.getTermType().equals(TermType.RECOMMENDED))
                .forEach(r -> {
                    if (handledLangs.containsKey(r.getLanguage())) {
                        addConstraintViolation(context, "too-many-recommended-terms", "terms");
                    }
                    handledLangs.put(r.getLanguage(), "");
                });

        if (handledLangs.isEmpty()) {
            addConstraintViolation(context, "should-have-one-recommended-term", "terms");
        }
        checkTermData(context, value.getTerms());
        return !isConstraintViolationAdded();
    }

    private void checkConceptData(ConstraintValidatorContext context, ConceptDTO dto) {
        checkCommonTextArea(context, dto.getChangeNote(), "changeNote");
        checkCommonTextArea(context, dto.getHistoryNote(), "historyNote");
        dto.getDefinition().forEach((key, value) -> checkCommonTextArea(context, value, "definition"));
        dto.getSubjectArea().forEach((key, value) -> checkCommonTextField(context, value, "subjectArea"));
        dto.getExamples().forEach(e -> checkCommonTextArea(context, e.getValue(), "examples"));
        dto.getNotes().forEach(e -> checkCommonTextArea(context, e.getValue(), "notes"));
        dto.getEditorialNotes().forEach(e -> checkCommonTextArea(context, e, "editorialNotes"));
        dto.getLinks().forEach(link -> {
            checkRequiredLocalizedValue(context, link.getName(), "links.name");
            checkNotNull(context, link.getUri(), "links.uri");
            link.getDescription().forEach((description, value) -> checkCommonTextArea(context, value, "links.description"));
        });
        dto.getSources().forEach(s -> checkCommonTextArea(context, s, "sources"));
        checkNotNull(context, dto.getStatus(), "status");
        dto.getReferences().forEach(r -> {
            checkNotNull(context, r.getConceptURI(), "references.conceptURI");
            checkNotNull(context, r.getReferenceType(), "references.referenceType");
        });
    }

    private void checkTermData(ConstraintValidatorContext context, Set<TermDTO> terms) {
        terms.forEach(term -> {
            checkCommonTextArea(context, term.getChangeNote(), "changeNote");
            checkCommonTextArea(context, term.getHistoryNote(), "usageHistory");
            checkCommonTextArea(context, term.getTermInfo(), "termInfo");
            checkNotNull(context, term.getLabel(), "label");
            checkCommonTextArea(context, term.getScope(), "scope");
            checkNotNull(context, term.getLanguage(), "language");
            checkNotNull(context, term.getStatus(), "status");
            checkCommonTextField(context, term.getTermStyle(), "termStyle");
        });
    }
}
