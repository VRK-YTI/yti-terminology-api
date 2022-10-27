package fi.vm.yti.terminology.api.validation;

import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.model.termed.*;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.yti.terminology.api.validation.ValidationConstants.*;

public class GenericDeleteAndSaveValidator extends BaseValidator implements
        ConstraintValidator<ValidGenericDeleteAndSave, GenericDeleteAndSave> {

    private static final List<String> STATUS_NAMES = Arrays.stream(Status.class.getEnumConstants())
                                                        .map(Enum::name)
                                                        .collect(Collectors.toList());

    /**
     * Checks if GenericSaveAndDelete object is valid
     * Either structural or content validation
     * @param deleteAndSave Generic delete and save object
     * @param context Constraint validator context
     * @return true if valid
     */
    @Override
    public boolean isValid(GenericDeleteAndSave deleteAndSave, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        Stream.concat(deleteAndSave.getSave().stream(), deleteAndSave.getPatch().stream()).forEach(node -> {
            //These checks are split into smaller functions to improve readability and documentation
            checkPropertiesAndPrefLabel(node, context);
            checkNodeType(node, context);
            checkLengths(node, context);
        });


        return !isConstraintViolationAdded();
    }


    /**
     * Checks if generic node has properties node and if the properties node contains prefLabel
     * Adds constraint violation if check doesn't pass
     * @param node Node to check
     * @param context Constraint validator context
     */
    private void checkPropertiesAndPrefLabel(GenericNode node, ConstraintValidatorContext context){
        final var prefLabel = "prefLabel";
        final var properties = node.getProperties();
        if (properties.isEmpty() ) {
            this.addConstraintViolation(context, MISSING_VALUE,"properties");
        }else if(!node.getType().getId().equals(NodeType.Concept)
                && (properties.get(prefLabel) == null || properties.get(prefLabel).isEmpty())){
            this.addConstraintViolation(context, MISSING_VALUE, prefLabel);
        }
    }

    /**
     * Checks if node has a node type,
     * if node type is Concept or Term check status
     * If node type is Concept check if it has a single prefLabelXl
     * @param node Node to check
     * @param context Constraint validator context
     */
    private void checkNodeType(GenericNode node, ConstraintValidatorContext context){
        //validate like vocabulary node if concept or term
        final var nodeType = node.getType().getId();
        var properties = node.getProperties();
        if (nodeType == null) {
            this.addConstraintViolation(context, MISSING_VALUE, "type");
        } else if (nodeType.equals(NodeType.Concept) || nodeType.equals(NodeType.Term)) {
            checkStatus(node, context);
            if (nodeType.equals(NodeType.Concept)) {
                checkConceptWordClass(properties, context);
                checkRelationships(node.getReferences(), context);
                checkConceptLanguageFields(properties, context);
                final var prefLabelXl = node.getReferences().get("prefLabelXl");
                if (prefLabelXl == null || prefLabelXl.isEmpty()) {
                    addConstraintViolation(context, MISSING_VALUE, "prefLabelXl");
                }
            }else{
                checkHomographNumber(properties, context);
                checkTermFamily(properties, context);
                checkTermEquivalency(properties, context);
                checkTermWordClass(properties, context);
                checkTermConjugation(properties, context);
            }
        }else if(nodeType.equals(NodeType.Collection)){
            checkCollectionPairCount(properties, context);
        }else if(nodeType.equals(NodeType.TerminologicalVocabulary) || nodeType.equals(NodeType.Vocabulary)){
            new VocabularyNodeValidator()
                    .isValid(node, context);
        }
    }

    /**
     * Check that properties of nodes are valid lengths
     * @param node Node to chekc
     * @param context Constraint validator context
     */
    private void checkLengths(GenericNode node, ConstraintValidatorContext context){
        var nodeType = node.getType().getId();
        if(nodeType != null){
            List<String> textFieldProperties = new ArrayList<>();
            List<String> textAreaProperties = new ArrayList<>();
            if(nodeType.equals(NodeType.Concept)){
                textFieldProperties = List.of("subjectArea", "conceptClass");
                textAreaProperties = List.of("definition", "changeNote", "example", "historyNote", "note", "source");
            }else if(nodeType.equals(NodeType.Term)){
                textFieldProperties = List.of("prefLabel", "termStyle");
                textAreaProperties = List.of("termInfo", "changeNote", "scope", "source", "historyNote", "editorialNote");
            }else if(nodeType.equals(NodeType.Collection)){
                textFieldProperties = List.of("prefLabel");
                textAreaProperties = List.of("definition");
            }
            //Skip checking if empty
            if(!textFieldProperties.isEmpty()){
                checkTextLength(textFieldProperties, TEXT_FIELD_MAX_LENGTH, node.getProperties(), context);
            }
            if(!textAreaProperties.isEmpty()){
                checkTextLength(textAreaProperties, TEXT_AREA_MAX_LENGTH, node.getProperties(), context);
            }
        }
    }

    /**
     * Checks the length of the values in for given properties
     * Serves as a helper function for {@link #checkLengths(GenericNode, ConstraintValidatorContext)}
     * @param textProperties Text properties to check
     * @param maxLength max length
     * @param properties node properties
     * @param context Constraint validator context
     */
    private void checkTextLength(List<String> textProperties, int maxLength, Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        textProperties.forEach(property -> {
            if(properties.containsKey(property) && !properties.get(property).isEmpty()
                    && properties.get(property).stream()
                    .anyMatch(val -> val.getValue().length() > maxLength)){
                addConstraintViolation(context, TOO_LONG_VALUE, property);
            }
        });
    }

    /**
     * Check that status exists and not empty,
     * Status must also be one of {@link Status}
     * @param node Node to check
     * @param context Constraint validator context
     */
    private void checkStatus(GenericNode node, ConstraintValidatorContext context){
        final var statusProperty = "status";
        final var status = node.getProperties().get(statusProperty);
        if (status == null || status.isEmpty()) {
            addConstraintViolation(
                    context,
                    MISSING_VALUE,
                    statusProperty);
        } else {
            // status must be one of Status enum
            if (status.size() != 1 || !STATUS_NAMES.contains(status.get(0).getValue())) {
                addConstraintViolation(context, INVALID_VALUE, statusProperty);
            }
        }
    }

    /**
     * Checks concept wordClass
     * In case Word class is added make sure its one of the available options
     * @param properties List of properties
     * @param context Constraint validator context
     */
    private void checkConceptWordClass(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var wordClass = "wordClass";
        //Check if wordClass is one of the available options, since wordClass is optional we only check when it exists
        if(properties.containsKey(wordClass) && !properties.get(wordClass).isEmpty()
                && properties.get(wordClass).stream()
                .anyMatch(d -> !d.getValue().isEmpty()
                           && !d.getValue().equals("adjective")
                           && !d.getValue().equals("verb"))
        ){
            addConstraintViolation(context, INVALID_VALUE, wordClass);
        }
    }

    /**
     * Checks concept relationships are ok.
     * @param references References
     * @param context Constraint validation context
     */
    private void checkRelationships(Map<String, List<Identifier>> references, ConstraintValidatorContext context){
        final var relationships = List.of("broader", "narrower", "related", "isPartOf", "hasPart", "relatedMatch", "related");
        for(String relationship : relationships){
            if(references.containsKey(relationship) && !references.get(relationship).isEmpty()
                    && references.get(relationship).stream()
                        .anyMatch(ref -> ref.getType().getId() == null
                                       || ref.getId() == null || ref.getType().getGraphId() == null)){
                    addConstraintViolation(context, INVALID_VALUE, relationship);
            }
        }
    }

    /**
     * Check concept fields with selectable language
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkConceptLanguageFields(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var propertiesWithLanguages = List.of("example", "note");
        for(String property : propertiesWithLanguages){
            if(properties.containsKey(property) && !properties.get(property).isEmpty()
                && properties.get(property).stream()
                    .anyMatch(lang -> (!lang.getLang().isEmpty() && lang.getValue().isEmpty())
                                    || lang.getLang().isEmpty() && !lang.getValue().isEmpty())){
                addConstraintViolation(context, INVALID_VALUE, property);
            }
        }
    }

    /**
     * Check if homograph number is empty or a number
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkHomographNumber(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var termHomographNumber = "termHomographNumber";
        if(properties.containsKey(termHomographNumber) && !properties.get(termHomographNumber).isEmpty()
            && properties.get(termHomographNumber).stream()
                .anyMatch(number -> !number.getValue().matches("(\\d)*"))){
            addConstraintViolation(context, INVALID_VALUE, termHomographNumber);
        }
    }

    /**
     * Check term family empty or one of specified
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkTermFamily(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var termFamily = "termFamily";
        if(properties.containsKey(termFamily) && !properties.get(termFamily).isEmpty()
                && properties.get(termFamily).stream()
                .anyMatch(style -> !style.getValue().isEmpty() && !style.getValue().equals("masculine")
                 && !style.getValue().equals("neutral") && !style.getValue().equals("feminine"))){
            addConstraintViolation(context, INVALID_VALUE, termFamily);
        }
    }

    /**
     * Check term equivalency empty or one of specified
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkTermEquivalency(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var termEquivalency = "termEquivalency";
        if(properties.containsKey(termEquivalency) && !properties.get(termEquivalency).isEmpty()
                && properties.get(termEquivalency).stream()
                .anyMatch(style -> !style.getValue().isEmpty() && !style.getValue().equals("<")
                        && !style.getValue().equals(">") && !style.getValue().equals("~"))){
            addConstraintViolation(context, INVALID_VALUE, termEquivalency);
        }
    }

    /**
     * Check term conjugation empty or one of specified
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkTermConjugation(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var termConjugation = "termConjugation";
        if(properties.containsKey(termConjugation) && !properties.get(termConjugation).isEmpty()
                && properties.get(termConjugation).stream()
                .anyMatch(style -> !style.getValue().isEmpty()
                        && !style.getValue().equals("singular")
                        && !style.getValue().equals("plural"))){
            addConstraintViolation(context, INVALID_VALUE, termConjugation);
        }
    }

    /**
     * Checks term wordClass
     * In case Word class is added make sure its one of the available options
     * @param properties List of properties
     * @param context Constraint validator context
     */
    private void checkTermWordClass(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var wordClass = "wordClass";
        //Check if wordClass is one of the available options, since wordClass is optional we only check when it exists
        if(properties.containsKey(wordClass) && !properties.get(wordClass).isEmpty()
                && properties.get(wordClass).stream()
                .anyMatch(d -> !d.getValue().isEmpty()
                        && !d.getValue().equals("adjective")
                        && !d.getValue().equals("verb")
        )){
            addConstraintViolation(context, INVALID_VALUE, wordClass);
        }
    }

    /**
     * Check that collection prefLabel and definition count match
     * @param properties Properties
     * @param context Constraint validator context
     */
    private void checkCollectionPairCount(Map<String, List<Attribute>> properties, ConstraintValidatorContext context){
        final var prefLabelCount = properties.get("prefLabel").stream().filter(prefLabel -> prefLabel.getValue() != null && !prefLabel.getValue().isEmpty()).count();
        final var definitionCount = properties.get("definition").stream().filter(definition -> definition.getValue() != null && !definition.getValue().isEmpty()).count();
        if(prefLabelCount == 0 || definitionCount == 0){
            addConstraintViolation(context, "prefLabel or definition cannot be empty", "prefLabel + definition");
        }

        if(prefLabelCount != definitionCount){
            addConstraintViolation(context, "prefLabel and definition count mismatch", "prefLabel + definition");
        }
    }
}