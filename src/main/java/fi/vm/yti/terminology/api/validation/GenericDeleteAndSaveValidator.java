package fi.vm.yti.terminology.api.validation;

import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.model.termed.Attribute;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.NodeType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.yti.terminology.api.validation.ValidationConstants.*;

public class GenericDeleteAndSaveValidator extends BaseValidator implements
        ConstraintValidator<ValidGenericDeleteAndSave, GenericDeleteAndSave> {

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
        if (nodeType == null) {
            this.addConstraintViolation(context, MISSING_VALUE, "type");
        } else if (nodeType.equals(NodeType.Concept) || nodeType.equals(NodeType.Term)) {
            checkStatus(node, context);
            if (nodeType.equals(NodeType.Concept)) {
                final var prefLabelXl = node.getReferences().get("prefLabelXl");
                if (prefLabelXl == null || prefLabelXl.size() != 1) {
                    addConstraintViolation(context, MISSING_VALUE, "prefLabelXl");
                }
            }
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
                textFieldProperties = List.of("prefLabel");
                textAreaProperties = List.of("termInfo", "changeNote", "scope", "source", "historyNote", "editorialNote");
            }else if(nodeType.equals(NodeType.Collection)){
                textFieldProperties = List.of("prefLabel");
                textAreaProperties = List.of("definition");
            }
            checkTextLength(textFieldProperties, TEXT_FIELD_MAX_LENGTH, node.getProperties(), context);
            checkTextLength(textAreaProperties, TEXT_AREA_MAX_LENGTH, node.getProperties(), context);
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
                addConstraintViolation(context, INVALID_VALUE, property);
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
            if (status.size() != 1 || !getStatusNames().contains(status.get(0).getValue())) {
                addConstraintViolation(context, INVALID_VALUE, statusProperty);
            }
        }
    }

    /**
     * Get Status enum names as list of strings
     * @return List of status names
     */
    private static List<String> getStatusNames() {
        return Arrays.stream(Status.class.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
