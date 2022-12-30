package fi.vm.yti.terminology.api.validation;

import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.frontend.TerminologyType;
import fi.vm.yti.terminology.api.model.termed.Attribute;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fi.vm.yti.terminology.api.validation.ValidationConstants.*;

public class VocabularyNodeValidator extends BaseValidator implements
        ConstraintValidator<ValidVocabularyNode, GenericNode> {

    @Override
    public boolean isValid(GenericNode genericNode, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        final var properties = genericNode.getProperties();
        if (properties.isEmpty()) {
            addConstraintViolation(context, MISSING_VALUE, "properties");
        }

        // language, languages properties required later so this is left as is
        final var languages = properties.get("language");
        if (languages == null || languages.isEmpty()) {
            addConstraintViolation(context, MISSING_VALUE,"language");
        }

        checkTerminologyType(properties, context);

        //Vocabulary type
        checkVocabularyType(genericNode, context);

        //Status
        checkStatus(properties, context);

        //PrefLabel
        checkPrefLabel(properties, context, languages);

        //description (optional)
        checkDescription(properties, context);

        //contact (optional)
        checkContact(properties, context);

        // references, checks requiring references only checked if references exist
        final var references = genericNode.getReferences();
        if (references == null || references.isEmpty()) {
            addConstraintViolation(context, MISSING_VALUE, "references");
        }else{
            //Contributor
            checkContributor(references, context);
            //InGroup
            checkInGroup(references, context);
        }

        return !isConstraintViolationAdded();
    }

    /**
     * Check if terminology type property is valid
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkTerminologyType(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var terminologyTypeProperty = "terminologyType";
        final var terminologyType = properties.get(terminologyTypeProperty);
        if (terminologyType == null) {
            addConstraintViolation(context, MISSING_VALUE, terminologyTypeProperty);
        }else if (terminologyType.size() != 1) {
            addConstraintViolation(context, INVALID_VALUE, terminologyTypeProperty);
        } else {
            final var validTypes = List.of(TerminologyType.TERMINOLOGICAL_VOCABULARY.name(),
                                                      TerminologyType.OTHER_VOCABULARY.name());
            if (!validTypes.contains(terminologyType.get(0).getValue())) {
                addConstraintViolation(context, INVALID_VALUE, terminologyTypeProperty);
            }
        }
    }

    private void checkVocabularyType(GenericNode node, ConstraintValidatorContext context){
        // type.id should always match TerminologicalVocabulary
        final var vocabularyType = node.getType();
        if (vocabularyType.getId() == null ||
                !vocabularyType.getId().toString().equals("TerminologicalVocabulary")) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    "type");
        }
    }

    /**
     * Check if prefLabel property is valid
     * @param properties Properties
     * @param context Constraint validator context
     * @param languages Languages
     */
    private void checkPrefLabel(Map<String, List<Attribute>> properties, ConstraintValidatorContext context, List<Attribute> languages){
        // list of language values, which will be used in later checks
        var langValues = languages == null ? Collections.emptyList() : languages.stream()
                .map(Attribute::getValue)
                .collect(Collectors.toList());

        //preflabel
        final var prefLabelProperty = "prefLabel";
        final var prefLabel = properties.get(prefLabelProperty);
        if (prefLabel == null || prefLabel.isEmpty()) {
            addConstraintViolation(context, MISSING_VALUE, prefLabelProperty);
        } else {
            // should have one label for each language
            var labelLanguages = prefLabel.stream()
                    .map(Attribute::getLang)
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEqualCollection(langValues, labelLanguages)) {
                addConstraintViolation(context, "Language mismatch", prefLabelProperty);
            }

            //PrefLabel for vocabulary should be max 150 characters
            if(prefLabel.stream().anyMatch(label -> label.getValue().trim().length() > TEXT_FIELD_MAX_LENGTH)){
                addConstraintViolation(context, INVALID_VALUE, prefLabelProperty);
            }

            // empty strings as values?
            if(prefLabel.stream().anyMatch(label -> label.getValue().trim().isEmpty())){
                addConstraintViolation(context, MISSING_VALUE, prefLabelProperty);
            }
        }
    }

    /**
     * Check if status property is valid
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkStatus(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var statusProperty = "status";
        final var status = properties.get(statusProperty);
        if (status == null || status.isEmpty()) {
            addConstraintViolation(context, MISSING_VALUE, statusProperty);
        } else {
            // status must be one of Status enum
            if (status.size() != 1 || !List.of(getStatusNames())
                    .contains(status.get(0).getValue())) {
                addConstraintViolation(context, INVALID_VALUE, statusProperty);
            }
        }
    }

    /**
     * Check if contact property is valid
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkContact(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var contact = "contact";
        //Check length, since contact is optional we only check when it exists
        if(properties.containsKey(contact) && !properties.get(contact).isEmpty()
            && properties.get(contact).stream()
                    .anyMatch(c -> c.getValue().length() > EMAIL_MAX_LENGTH)){
            addConstraintViolation(context, INVALID_VALUE, contact);
        }
    }

    /**
     * Check if description property is valid
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkDescription(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var description = "description";
        //Check length, since description is optional we only check when it exists
        if(properties.containsKey(description) && !properties.get(description).isEmpty()
                && properties.get(description).stream()
                .anyMatch(d -> d.getValue().length() > TEXT_AREA_MAX_LENGTH)){
            addConstraintViolation(context, INVALID_VALUE, description);
        }
    }

    /**
     * Check if contributor reference is valid
     * @param references References
     * @param context Constraint validator context
     */
    private void checkContributor(Map<String, List<Identifier>> references, ConstraintValidatorContext context){
        final var contributor = "contributor";
        if(!references.containsKey(contributor) || references.get(contributor).isEmpty()) {
            addConstraintViolation(context, MISSING_VALUE, contributor);
        }
    }

    /**
     * Check if inGroup reference is valid
     * @param references References
     * @param context Constraint validator context
     */
    private void checkInGroup(Map<String, List<Identifier>> references, ConstraintValidatorContext context){
        final var inGroup = "inGroup";
        if(!references.containsKey(inGroup) || references.get(inGroup).isEmpty()) {
            addConstraintViolation(context, MISSING_VALUE, inGroup);
        }
    }

    /**
     * Get Status enum names as list of strings
     * @return List of status names
     */
    private static String[] getStatusNames() {
        return Arrays.stream(Status.class.getEnumConstants())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
