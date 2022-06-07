package fi.vm.yti.terminology.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class VocabularyNotFoundException extends RuntimeException {

    public VocabularyNotFoundException(String prefix) {
        super("Vocabulary not found with prefix: " + prefix);
    }

    public VocabularyNotFoundException(UUID id) {
        super("Vocabulary not found with id: " + id);
    }
}
