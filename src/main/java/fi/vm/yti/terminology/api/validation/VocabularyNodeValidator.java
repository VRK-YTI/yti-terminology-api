package fi.vm.yti.terminology.api.validation;

import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.frontend.TerminologyType;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VocabularyNodeValidator implements
        ConstraintValidator<ValidVocabularyNode, GenericNode>, Annotation {

    private boolean constraintViolationAdded;

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    @Override
    public boolean isValid(GenericNode genericNode, ConstraintValidatorContext context) {
        this.constraintViolationAdded = false;

        final var properties = genericNode.getProperties();
        if (properties.size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "properties");
        }

        //
        // language
        //
        final var languages = properties.get("language");
        if (languages == null || languages.size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "language");
        }

        //
        // terminologyType
        //
        //
        final var terminologyType = properties.get("terminologyType");
        if (terminologyType == null) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "terminologyType");
        }
        else if (terminologyType.size() != 1) {
            this.addConstraintViolation(
                    context,
                    "Invalid value",
                    "terminologyType");
        } else {
            final var validTypes = new String[]{
                    TerminologyType.TERMINOLOGICAL_VOCABULARY.name(),
                    TerminologyType.OTHER_VOCABULARY.name()
            };
            if (!Arrays.asList(validTypes).contains(terminologyType.get(0).getValue())) {
                this.addConstraintViolation(
                        context,
                        "Invalid value",
                        "terminologyType");
            }
        }

        // type.id should always match TerminologicalVocabulary
        final var vocabularyType = genericNode.getType();
        if (vocabularyType.getId() == null ||
                !vocabularyType.getId().toString().equals("TerminologicalVocabulary")) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "type");
        }

        //
        // status
        //
        final var status = properties.get("status");
        if (status == null || status.size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "status");
        } else {
            // status must be one of Status enum
            if (status.size() != 1 || !List.of(getStatusNames())
                    .contains(status.get(0).getValue())) {
                this.addConstraintViolation(
                        context,
                        "Invalid value",
                        "status");
            }
        }

        // list of language values, which will be used in later checks
        var langValues = languages.stream()
                .map(lang -> lang.getValue())
                .collect(Collectors.toList());

        //
        // prefLabel
        //
        final var prefLabel = properties.get("prefLabel");
        if (prefLabel == null || prefLabel.size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "prefLabel");
        } else {
            // should have one label for each language
            var labelLanguages = prefLabel.stream()
                    .map(label -> label.getLang())
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEqualCollection(langValues, labelLanguages)) {
                this.addConstraintViolation(
                        context,
                        "Language mismatch",
                        "prefLabel");
            }

            // empty strings as values?
            var emptyValues = prefLabel.stream()
                    .filter(label -> label.getValue().trim().isEmpty())
                    .collect(Collectors.toList());
            if (emptyValues.size() > 0) {
                this.addConstraintViolation(
                        context,
                        "Missing value",
                        "prefLabel");
            }
        }

        //
        // references
        //
        final var references = genericNode.getReferences();
        if (references == null || references.size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "references");
        }

        // contributor
        if (!references.containsKey("contributor") || references.get("contributor").size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "contributor");
        }

        // inGroup
        if (!references.containsKey("inGroup") || references.get("inGroup").size() == 0) {
            this.addConstraintViolation(
                    context,
                    "Missing value",
                    "inGroup");
        }

        return !this.constraintViolationAdded;
    }

    private void addConstraintViolation(
            ConstraintValidatorContext context,
            String message,
            String property) {

        if (!this.constraintViolationAdded) {
            context.disableDefaultConstraintViolation();
            this.constraintViolationAdded = true;
        }

        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }

    private static String[] getStatusNames() {
        return Arrays.stream(Status.class.getEnumConstants())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
