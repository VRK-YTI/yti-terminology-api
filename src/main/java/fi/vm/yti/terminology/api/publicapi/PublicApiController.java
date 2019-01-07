package fi.vm.yti.terminology.api.publicapi;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiParam;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/terminology/publicapi")
public class PublicApiController {

    private static final Logger logger = LoggerFactory.getLogger(PublicApiController.class);
    private final PublicApiTermedService termedService;
    private final PublicApiElasticSearchService publicApiElasticSearchService;

    public PublicApiController(PublicApiTermedService termedService,
                               PublicApiElasticSearchService publicApiElasticSearchService) {
        this.termedService = termedService;
        this.publicApiElasticSearchService = publicApiElasticSearchService;
    }

    @RequestMapping(value = "/vocabularies", method = GET, produces = APPLICATION_JSON_VALUE)
    List<PublicApiVocabulary> getVocabularyList() {
        logger.info("GET /vocabularies requested");
        return termedService.getVocabularyList();
    }

    @RequestMapping(value = "/searchconcept/searchterm/{searchTerm}/vocabulary/{vocabularyId}", method = GET, produces = APPLICATION_JSON_VALUE)
    List<PublicApiConcept> searchConceptWithStatus(@ApiParam(value = "Serch term for elastic search.") @PathVariable String searchTerm,
                                                   @ApiParam(value = "Vocabulary ID.") @PathVariable String vocabularyId,
                                                   @ApiParam(value = "Status for filtering. If missing,  show all.") @RequestParam(required = false) String status) {

        logger.info("GET /searchconcept/searchterm/{searchTerm}/vocabulary/{vocabularyId} requested");
        return publicApiElasticSearchService.searchConcept(searchTerm, vocabularyId, status);
    }
}
