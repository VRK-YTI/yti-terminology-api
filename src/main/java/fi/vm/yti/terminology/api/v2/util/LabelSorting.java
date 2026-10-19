package fi.vm.yti.terminology.api.v2.util;

import fi.vm.yti.common.Constants;
import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public final class LabelSorting {

    private LabelSorting() {}

    public static <T> Comparator<T> byLabel(String language,
                                            Function<T, Map<String, String>> labelGetter,
                                            Function<T, String> tieBreaker) {
        var lang = (language == null || language.isBlank()) ? Constants.DEFAULT_LANGUAGE : language;
        var collator = Collator.getInstance(Locale.forLanguageTag(lang));
        collator.setStrength(Collator.SECONDARY);
        return Comparator
                .comparing((T t) -> pickLabel(labelGetter.apply(t), lang),
                        Comparator.nullsLast(collator::compare))
                .thenComparing(tieBreaker, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    public static String pickLabel(Map<String, String> labels, String lang) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        return Stream.of(lang, Constants.DEFAULT_LANGUAGE, "en")
                .map(labels::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> labels.values().iterator().next());
    }
}
