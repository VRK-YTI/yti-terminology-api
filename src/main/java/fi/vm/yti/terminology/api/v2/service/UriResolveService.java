package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.terminology.api.v2.migration.v1.Termed;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;

@Service
public class UriResolveService {

    private final TerminologyRepository repository;

    @Value("${env:}")
    private String awsEnv;

    public UriResolveService(TerminologyRepository repository) {
        this.repository = repository;
    }

    public ResponseEntity<String> resolve(String iri, String accept) {

        if (checkIRI(iri)) {
            return ResponseEntity.badRequest().build();
        }
        var currentUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
        var redirectURL = UriComponentsBuilder.newInstance()
                .scheme(currentUrl.getScheme())
                .host(currentUrl.getHost());

        if (currentUrl.getHost().equals("localhost")) {
            redirectURL.port(3000);
        }

        var terminologyURI = TerminologyURI.Factory.fromUri(iri);

        var prefix = getIdentifier(terminologyURI.getPrefix());
        var resourceId = getIdentifier(terminologyURI.getResourceId());

        if (accept != null && accept.contains(MimeTypeUtils.TEXT_HTML_VALUE)) {
            redirectURL.pathSegment("terminology", prefix);
            if (resourceId != null) {
                var resourceType = getResourceType(TerminologyURI.Factory
                        .createConceptURI(prefix, resourceId)
                        .getResourceURI());
                redirectURL.pathSegment(resourceType, resourceId);
            }
        } else {
            redirectURL.pathSegment("terminology-api", "v2", "export", prefix);
        }
        return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, redirectURL.toUriString())
                .build();
    }

    public String resolveLegacyURL(String termedId) {

        var select = new SelectBuilder();
        select.addVar("?subject")
                .addGraph("?g",
                        new WhereBuilder().addWhere("?subject", Termed.id, termedId));

        var result = new ArrayList<String>();
        repository.querySelect(select.build(), row -> result.add(row.get("subject").toString()));

        if (result.isEmpty()) {
            throw new ResourceNotFoundException(termedId);
        }

        if (!awsEnv.isBlank()) {
            return result.get(0) + "?env=" + awsEnv;
        }
        return result.get(0);
    }

    private boolean checkIRI(String iri) {
        return iri == null || !iri.startsWith(Constants.TERMINOLOGY_NAMESPACE);
    }

    private String getResourceType(String resourceURI) {
        var select = new SelectBuilder()
                .addVar("?type")
                .addGraph("?g", new WhereBuilder()
                        .addWhere(NodeFactory.createURI(resourceURI), RDF.type, "?type")
                );
        var result = new ArrayList<String>();
        repository.querySelect(select.build(), row -> result.add(row.get("type").toString()));

        String resourceType;
        if (result.isEmpty()) {
            throw new ResourceNotFoundException(resourceURI);
        } else if (result.get(0).equals(SKOS.Concept.getURI())) {
            resourceType = "concept";
        } else if (result.get(0).equals(SKOS.Collection.getURI())) {
            resourceType = "collection";
        } else {
            throw new ResourceNotFoundException(resourceURI);
        }
        return resourceType;
    }

    /**
     * add prefix a to identifier if starting with digit
     * @param identifier resource identifier or terminology prefix
     * @return prefixed id
     */
    private String getIdentifier(String identifier) {
        if (identifier != null && Character.isDigit(identifier.charAt(0))) {
            return "a" + identifier;
        }
        return identifier;
    }
}
