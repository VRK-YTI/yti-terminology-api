package fi.vm.yti.terminology.api.importapi;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.exception.ExcelParseException;
import fi.vm.yti.terminology.api.exception.NamespaceInUseException;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.importapi.ImportStatusResponse.ImportStatus;
import fi.vm.yti.terminology.api.importapi.excel.ExcelParser;
import fi.vm.yti.terminology.api.importapi.excel.TerminologyImportDTO;
import fi.vm.yti.terminology.api.importapi.simpleexcel.SimpleExcelParser;
import fi.vm.yti.terminology.api.migration.DomainIndex;
import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.JsonUtils;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
@EnableJms
public class ImportService {

    public static final class ImportResponse {
        private String jobtoken;

        public ImportResponse(final String jobtoken) {
            this.jobtoken = jobtoken;
        }

        public String getJobtoken() {
            return jobtoken;
        }

        public void setJobtoken(final String jobtoken) {
            this.jobtoken = jobtoken;
        }

        @Override
        public String toString() {
            return "{\"jobtoken\":\"" + jobtoken + "\"}";
        }
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportService.class);

    private final FrontendGroupManagementService groupManagementService;
    private final FrontendTermedService termedService;
    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationManager authorizationManager;
    private final YtiMQService ytiMQService;

    /**
     * Map containing metadata types. used  when creating nodes.
     */
    private HashMap<String,MetaNode> typeMap = new HashMap<>();

    private final String subSystem;
    private final Integer batchSize;

    @Autowired
    public ImportService(FrontendGroupManagementService groupManagementService,
                         FrontendTermedService frontendTermedService,
                         AuthenticatedUserProvider userProvider,
                         AuthorizationManager authorizationManager,
                         YtiMQService ytiMQService,
                         @Value("${mq.active.subsystem}") String subSystem,
                         @Value("${mq.batch.size:100}") Integer batchSize) {
        this.groupManagementService = groupManagementService;
        this.termedService = frontendTermedService;
        this.userProvider = userProvider;
        this.authorizationManager = authorizationManager;
        this.subSystem = subSystem;
        this.ytiMQService = ytiMQService;
        this.batchSize = batchSize;
    }

    ResponseEntity<String> getStatus(UUID jobtoken, boolean full){
        // Status not_found/running/errors
        // Query status information from ActiveMQ
        HttpStatus status;
        StringBuffer statusString= new StringBuffer();
        ImportStatusResponse response = new ImportStatusResponse();

        // Get always full state
        status = ytiMQService.getStatus(jobtoken, statusString);

        // Construct  response
        if (status == HttpStatus.OK) {
            response = ImportStatusResponse.fromString(statusString.toString());
            if (!full) {
                // Remove status messages if not needed
                response.getStatusMessage().clear();
            }
            response.setStatus(ImportStatus.SUCCESS);
        } else if (status == HttpStatus.NOT_ACCEPTABLE) {
                response.setStatus(ImportStatus.FAILURE);
                response.getStatusMessage().clear();
                response.addStatusMessage(new ImportStatusMessage("Vocabulary","Import operation already started"));
        } else if (status == HttpStatus.PROCESSING) {
            response = ImportStatusResponse.fromString(statusString.toString());
            if (!full) {
                // Remove status messages if not needed
                response.getStatusMessage().clear();
            }
            response.setStatus(ImportStatus.PROCESSING);
        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            response = ImportStatusResponse.fromString(statusString.toString());
            response.setStatus(ImportStatus.FAILURE);
        } else {
            response.setStatus(ImportStatus.NOT_FOUND);
        }
        // Construct return message
        JsonUtils.prettyPrintJson(response);

        return new ResponseEntity<>(JsonUtils.prettyPrintJsonAsString(response), HttpStatus.OK);
    }

    ResponseEntity<String> checkIfImportIsRunning(String uri){
        System.out.println("CheckIfRunning");
        boolean status = ytiMQService.checkIfImportIsRunning(uri);
        System.out.println("CheckIfRunning - "+status);
        if(status)
            return new ResponseEntity<>("{\"status\":\"Running\"}", HttpStatus.OK);
        return new ResponseEntity<>("{\"status\":\"Stopped\"}", HttpStatus.OK);
    }

    ResponseEntity<String> handleNtrfDocumentAsync(String format, UUID vocabularyId, MultipartFile file) {
        String rv;
        System.out.println("Incoming vocabularity= "+vocabularyId+" - file:"+file.getName()+" size:"+file.getSize()+ " type="+file.getContentType());
        // Fail if given format string is not ntrf
        if (!format.equals("ntrf")) {
            LOGGER.error("Unsupported format:<{}> (Currently supported formats: ntrf)", format);
            // Unsupported format
            return new ResponseEntity<>("Unsupported format:<" + format + ">    (Currently supported formats: ntrf)\n", HttpStatus.NOT_ACCEPTABLE);
        }

        Graph vocabulary = null;

        // Get vocabularity
        try {
            vocabulary = termedService.getGraph(vocabularyId);
            // Import running for given vocabulary, drop it
            if(ytiMQService.checkIfImportIsRunning(vocabulary.getUri())){
                LOGGER.error("Import running for Vocabulary:<{}>", vocabularyId);
                return new ResponseEntity<>("Import running for Vocabulary:<" + vocabularyId+">", HttpStatus.CONFLICT);
            }
        } catch ( NullPointerException nex){
            // Vocabularity not found
            LOGGER.error("Vocabulary:<{}> not found", vocabularyId);
            return new ResponseEntity<>("Vocabulary:<" + vocabularyId + "> not found\n", HttpStatus.NOT_FOUND);
        }

        UUID operationId=UUID.randomUUID();
        if(!file.getContentType().equalsIgnoreCase("text/xml")){
            rv = "{\"operation\":\""+operationId+"\", \"error\":\"incoming file type  is wrong\"}";
            return new ResponseEntity<>( rv, HttpStatus.BAD_REQUEST);
        }
        rv = new ImportResponse(operationId.toString()).toString();
        // Handle incoming xml
        try {
            ytiMQService.setStatus(YtiMQService.STATUS_PREPROCESSING, operationId.toString(), userProvider.getUser().getId().toString(), vocabulary.getUri(),"Validating");
            JAXBContext jc = JAXBContext.newInstance(VOCABULARY.class);
            // Disable DOCTYPE-directive from incoming file.
            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader xsr = xif.createXMLStreamReader(file.getInputStream());
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            // At last, resolve ntrf-POJO's
            VOCABULARY voc = (VOCABULARY) unmarshaller.unmarshal(xsr);

            // All set up, execute actual import
            List<?> l = voc.getRECORDAndHEADAndDIAG();
            System.out.println("Incoming objects count=" + l.size());
            ImportStatusResponse response = new ImportStatusResponse();
            response.setStatus(ImportStatus.PREPROCESSING);
            response.addStatusMessage(new ImportStatusMessage("Vocabulary",l.size()+" items validated"));
            response.setProcessingTotal(l.size());
            ytiMQService.setStatus(YtiMQService.STATUS_PROCESSING, operationId.toString(), userProvider.getUser().getId().toString(), vocabulary.getUri(),response.toString());
            StringWriter sw = new StringWriter();
            marshaller.marshal(voc, sw);
            // Add application specific headers
            MessageHeaderAccessor accessor = new MessageHeaderAccessor();
            accessor.setHeader("vocabularyId",vocabularyId.toString());
            accessor.setHeader("format","NTRF");
            int stat = ytiMQService.handleImportAsync(operationId, accessor, subSystem, vocabulary.getUri(), sw.toString());
            if(stat != HttpStatus.OK.value()){
                System.out.println("Import failed code:"+stat);
            }
        } catch (IOException ioe){
            System.out.println("Incoming transform error=" + ioe);
        } catch (XMLStreamException se) {
            System.out.println("Incoming transform error=" + se);
        } catch (JAXBException je){
            System.out.println("Incoming transform error=" + je);
        }
        return new ResponseEntity<>( rv, HttpStatus.OK);
    }

    public UUID handleSimpleExcelImport(UUID terminologyId, InputStream is) throws NullPointerException, IOException {
        boolean exists = terminologyExists(terminologyId);
        if (!exists) {
            throw new NullPointerException("Terminology doesnt exist");
        }

        var graph = termedService.getGraph(terminologyId);
        List<String> languages = graph.getProperties().get("prefLabel").stream().map(Property::getLang).collect(Collectors.toList());

        var parser = new SimpleExcelParser();
        XSSFWorkbook workbook = parser.getWorkbook(is);
        List<GenericNode> nodes = parser.buildNodes(workbook, terminologyId, languages);

        check(authorizationManager.canModifyNodes(nodes));
        //JOB
        var jobToken = UUID.randomUUID();
        var accessor = new MessageHeaderAccessor();
        accessor.setHeader("vocabularyId", terminologyId.toString());
        accessor.setHeader("format", "EXCEL");

        List<List<GenericNode>> batches = ImportUtil.getBatches(nodes, batchSize);

        ytiMQService.handleExcelImportAsync(jobToken, accessor, graph.getUri(), batches);

        return jobToken;
    }

    public UUID handleExcelImport(InputStream is) {
        ZipSecureFile.setMinInflateRatio(0.0001);
        ExcelParser parser = new ExcelParser();
        try {
            // Map information domain names with uuid
            Map<String, String> groupMap = getGroupMap();

            // If organization id is not present in excel file, add user's organizations as a default
            List<String> userOrganizations = getUserOrganizations();

            XSSFWorkbook workbook = parser.getWorkbook(is);

            TerminologyImportDTO dto = parser.buildTerminologyNode(workbook, groupMap, userOrganizations);
            check(authorizationManager.canCreateVocabulary(dto.getTerminologyNode()));

            // Check if graph exists
            UUID graphId = dto.getTerminologyNode().getType().getGraphId();
            boolean exists = terminologyExists(graphId);

            if (!exists) {
                // if not exists, check that namespace is available
                if (termedService.isNamespaceInUse(dto.getNamespace())) {
                    throw new NamespaceInUseException();
                }
                // new terminology is created if not exist
                termedService.createVocabulary(
                        DomainIndex.TERMINOLOGICAL_VOCABULARY_TEMPLATE_GRAPH_ID,
                        dto.getNamespace(),
                        dto.getTerminologyNode(),
                        graphId,
                        true
                );
            }

            List<GenericNode> conceptsAndTerms = new ArrayList<>();

            conceptsAndTerms.add(dto.getTerminologyNode());

            conceptsAndTerms.addAll(parser.buildConceptNodes(workbook,
                    dto.getNamespace(),
                    graphId,
                    dto.getLanguages()
            ));
            conceptsAndTerms.addAll(parser.buildTermNodes(workbook,
                    dto.getNamespace(),
                    graphId,
                    dto.getLanguages()
            ));

            // Add collection nodes separately because concepts must exist before they can be saved
            List<GenericNode> collectionNodes = parser.buildCollectionNodes(workbook,
                    dto.getNamespace(),
                    graphId,
                    dto.getLanguages()
            );

            UUID jobToken = UUID.randomUUID();

            MessageHeaderAccessor accessor = new MessageHeaderAccessor();
            accessor.setHeader("vocabularyId", graphId.toString());
            accessor.setHeader("format", "EXCEL");

            List<List<GenericNode>> batches = ImportUtil.getBatches(conceptsAndTerms, batchSize);

            if (!collectionNodes.isEmpty()) {
                batches.addAll(ImportUtil.getBatches(collectionNodes, batchSize));
            }

            ytiMQService.handleExcelImportAsync(jobToken, accessor, dto.getTerminologyNode().getUri(), batches);

            return jobToken;
        } catch (ExcelParseException | NamespaceInUseException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private boolean terminologyExists(UUID graphId) {
        try {
            termedService.getGraph(graphId);
            return true;
        } catch (NullPointerException ne) {
            // NullPointerException is thrown if graph doesn't exist
        }
        return false;
    }

    private List<String> getUserOrganizations() {
        YtiUser user = userProvider.getUser();
        return user.getOrganizations(Role.ADMIN, Role.TERMINOLOGY_EDITOR)
                .stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
    }

    private Map<String, String> getGroupMap() {
        Map<String, String> groupMap = new HashMap<>();
        JsonNode groups = termedService.getNodeListWithoutReferencesOrReferrers(NodeType.Group);
        for (JsonNode node : groups) {
            groupMap.put(
                    node.get("properties").get("notation").get(0).get("value").textValue(),
                    node.get("id").textValue()
            );
        }
        return groupMap;
    }



    @PreDestroy
    public void onDestroy() throws Exception {
        System.out.println("Spring Container is destroyed!");
    }
}
