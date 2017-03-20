package fi.csc.termed.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@RestController
public class ReceiveNotificationController {

    private final ElasticSearchService elasticSearchService;

    @Autowired
    public ReceiveNotificationController(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @RequestMapping(value = "/notify")
    public void notify(@RequestParam Notification notification) throws IOException, InterruptedException {
        this.elasticSearchService.updateIndex(notification);
    }
}
