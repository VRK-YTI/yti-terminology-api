package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.ReferenceMeta;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import org.jetbrains.annotations.NotNull;

import static fi.vm.yti.terminology.api.migration.DomainIndex.GROUP_DOMAIN;
import static fi.vm.yti.terminology.api.migration.DomainIndex.ORGANIZATION_DOMAIN;
import static fi.vm.yti.terminology.api.migration.PropertyUtil.*;
import static java.util.Collections.emptyMap;

public final class ReferenceIndex {

    private static final TypeId termDomainFromConceptDomain(TypeId conceptDomain) {
        return new TypeId(NodeType.Term, conceptDomain.getGraph());
    }

    @NotNull
    public static ReferenceMeta contributor(TypeId domain, long index) {
        return new ReferenceMeta(
                ORGANIZATION_DOMAIN,
                "contributor",
                "http://purl.org/dc/terms/contributor",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Sisällöntuottaja",
                        "Contributor"
                )
        );
    }

    @NotNull
    public static ReferenceMeta group(TypeId domain, long index) {
        return new ReferenceMeta(
                GROUP_DOMAIN,
                "inGroup",
                "http://purl.org/dc/terms/isPartOf",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Luokitus",
                        "Classification"
                )
        );
    }

    @NotNull
    public static ReferenceMeta broader(TypeId domain, long index, String fi, String en) {
        return new ReferenceMeta(
                domain,
                "broader",
                "http://www.w3.org/2004/02/skos/core#broader",
                index,
                domain,
                emptyMap(),
                prefLabel(fi, en)
        );
    }

    @NotNull
    public static ReferenceMeta narrower(TypeId domain, long index, String fi, String en) {
        return new ReferenceMeta(
                domain,
                "narrower",
                "http://www.w3.org/2004/02/skos/core#narrower",
                index,
                domain,
                emptyMap(),
                prefLabel(fi, en)
        );
    }

    @NotNull
    public static ReferenceMeta relatedConcept(TypeId domain, long index) {
        return new ReferenceMeta(
                domain,
                "related",
                "http://www.w3.org/2004/02/skos/core#related",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Liittyvä käsite",
                        "Related concept"
                )
        );
    }

    @NotNull
    public static ReferenceMeta partOfConcept(TypeId domain, long index) {
        return new ReferenceMeta(
                domain,
                "isPartOf",
                "http://purl.org/dc/terms/partOf",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Koostumussuhteinen yläkäsite",
                        "Is part of concept"
                )
        );
    }

    @NotNull
    public static ReferenceMeta hasPartConcept(TypeId domain, long index) {
        return new ReferenceMeta(
                domain,
                "hasPart",
                "http://purl.org/dc/terms/hasPart",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Koostumussuhteinen alakäsite",
                        "Has part concept"
                )
        );
    }

    @NotNull
    public static ReferenceMeta relatedMatch(TypeId domain, TypeId externalLinkDomain, long index) {
        return new ReferenceMeta(
                externalLinkDomain,
                "relatedMatch",
                "http://www.w3.org/2004/02/skos/core#relatedMatch",
                index,
                domain,
                emptyMap(),
                merge(
                        prefLabel(
                                "Liittyvä käsite toisessa sanastossa",
                                "Related concept in other vocabulary"
                        ),
                        type("link")
                )
        );
    }

    @NotNull
    public static ReferenceMeta exactMatch(TypeId domain, TypeId externalLinkDomain, long index) {
        return new ReferenceMeta(
                externalLinkDomain,
                "exactMatch",
                "http://www.w3.org/2004/02/skos/core#exactMatch",
                index,
                domain,
                emptyMap(),
                merge(
                        prefLabel(
                                "Vastaava käsite toisessa sanastossa",
                                "Related concept from other vocabulary"
                        ),
                        type("link")
                )
        );
    }

    @NotNull
    public static ReferenceMeta closeMatch(TypeId domain, TypeId externalLinkDomain, long index) {
        return new ReferenceMeta(
                externalLinkDomain,
                "closeMatch",
                "http://www.w3.org/2004/02/skos/core#closeMatch",
                index,
                domain,
                emptyMap(),
                merge(
                        prefLabel(
                                "Lähes vastaava käsite toisessa sanastossa",
                                "Close match in other vocabulary"
                        ),
                        type("link")
                )
        );
    }

    @NotNull
    public static ReferenceMeta member(TypeId domain, TypeId targetDomain, long index) {
        return new ReferenceMeta(
                targetDomain,
                "member",
                "http://www.w3.org/2004/02/skos/core#member",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Valikoimaan kuuluva käsite",
                        "Member"
                )
        );
    }

    @NotNull
    public static ReferenceMeta prefLabelXl(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "prefLabelXl",
                "http://www.w3.org/2008/05/skos-xl#prefLabel",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Suositettava termi",
                        "Preferred term"
                )
        );
    }

    @NotNull
    public static ReferenceMeta altLabelXl(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "altLabelXl",
                "http://uri.suomi.fi/datamodel/ns/st#synonym",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Synonyymi",
                        "Synonym"
                )
        );
    }

    @NotNull
    public static ReferenceMeta notRecommendedSynonym(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "notRecommendedSynonym",
                "http://uri.suomi.fi/datamodel/ns/st#notRecommendedSynonym",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Ei-suositeltava synonyymi",
                        "Non-recommended synonym"
                )
        );
    }

    @NotNull
    public static ReferenceMeta hiddenTerm(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "hiddenTerm",
                "http://uri.suomi.fi/datamodel/ns/st#hiddenTerm",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Ohjaustermi",
                        "Hidden term"
                )
        );
    }

    @NotNull
    public static ReferenceMeta searchTerm(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "searchTerm",
                "http://uri.suomi.fi/datamodel/ns/st#searchTerm",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Hakutermi",
                        "Search term"
                )
        );
    }

    @NotNull
    public static ReferenceMeta narrowMatch(TypeId domain, TypeId externalLinkDomain, long index) {
        return new ReferenceMeta(
                externalLinkDomain,
                "narrowMatch",
                "http://www.w3.org/2004/02/skos/core#narrowMatch",
                index,
                domain,
                emptyMap(),
                merge(
                        prefLabel(
                                "Alakäsite toisessa sanastossa",
                                "Narrower concept in other vocabulary"
                        ),
                        type("link")
                )
        );
    }

    @NotNull
    public static ReferenceMeta broadMatch(TypeId domain, TypeId externalLinkDomain, long index) {
        return new ReferenceMeta(
                externalLinkDomain,
                "broadMatch",
                "http://www.w3.org/2004/02/skos/core#broadMatch",
                index,
                domain,
                emptyMap(),
                merge(
                        prefLabel(
                                "Yläkäsite toisessa sanastossa",
                                "Broader concept in other vocabulary"
                        ),
                        type("link")
                )
        );
    }

    // prevent construction
    private ReferenceIndex() {
    }
}
