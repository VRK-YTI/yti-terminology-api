package fi.vm.yti.terminology.api.importapi;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.springframework.http.HttpMethod.POST;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.stream.Collectors;

import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.model.ntrf.*;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.resolve.ResolveService;
import jakarta.xml.bind.JAXBElement;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.importapi.ImportStatusMessage.Level;
import fi.vm.yti.terminology.api.importapi.ImportStatusResponse.ImportStatus;
import fi.vm.yti.terminology.api.util.JsonUtils;
import fi.vm.yti.terminology.api.util.Parameters;

@Component
public class NtrfMapper {

    private static final String USER_PASSWORD = "user";
    private static final UUID NULL_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final TermedRequester termedRequester;
    private final FrontendTermedService termedService;
    private final AuthenticatedUserProvider userProvider;
    private final YtiMQService ytiMQService;
    private final ResolveService resolveService;

    /**
     * Map containing metadata types. used when creating nodes.
     */
    private final HashMap<String, MetaNode> typeMap = new HashMap<>();
    /**
     * Map containing node.code or node.uri as a key and UUID as a value. Used for
     * matching existing items and updating them instead of creating new ones
     */
    private final HashMap<String, UUID> idMap = new HashMap<>();
    private final HashMap<UUID, String> reverseIdMap = new HashMap<>();
    /**
     * Map containing node.code or node.uri as a key and UUID as a value. Used for
     * reference resolving after all concepts and terms are created
     */
    private final HashMap<String, UUID> createdIdMap = new HashMap<>();

    /**
     * Map binding together reference string and external URL fromn ntrf
     * SOURF-element
     */
    private final HashMap<String, HashMap<String, String>> referenceMap = new HashMap<>();

    /**
     * Map for NCON/RCON-reference cache. Operation targetId,
     * type(generic/partitive), broaderConceptId
     */
    private final Map<String, List<ConnRef>> nconList = new LinkedHashMap<>();
    private final Map<String, List<ConnRef>> rconList = new LinkedHashMap<>();
    private final Map<String, List<ConnRef>> bconList = new LinkedHashMap<>();

    private String currentRecord;
    private final List<StatusMessage> statusList = new ArrayList<>();

    int errorCount = 0;

    private static final Logger logger = LoggerFactory.getLogger(NtrfMapper.class);

    @Autowired
    public NtrfMapper(TermedRequester termedRequester,
            FrontendTermedService frontendTermedService,
            AuthenticatedUserProvider userProvider,
            YtiMQService ytiMQService,
            ResolveService resolveService) {
        this.termedRequester = termedRequester;
        this.termedService = frontendTermedService;
        this.userProvider = userProvider;
        this.ytiMQService = ytiMQService;
        this.resolveService = resolveService;
    }

    private boolean updateAndDeleteInternalNodes(UUID userId, GenericDeleteAndSave deleteAndSave, boolean sync) {

        boolean rv = true;
        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", String.valueOf(sync));
        try {
            this.termedRequester.exchange("/nodes", POST, params, String.class, deleteAndSave, userId.toString(),
                    USER_PASSWORD);
        } catch (HttpServerErrorException ex) {
            logger.error(ex.getResponseBodyAsString());
            String error = ex.getResponseBodyAsString();

            Pattern pairRegex = Pattern
                    .compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");
            Matcher matcher = pairRegex.matcher(error);
            List<UUID> reflist = new ArrayList<>();
            while (matcher.find()) {
                String a = matcher.group(0);
                reflist.add(UUID.fromString(a));
            }
            if (reflist.size() > 1) {
                logger.error("Failed UUID={} Code:{}", reflist.get(1), reverseIdMap.get(reflist.get(1)));
            }
            statusList
                    .add(new StatusMessage(Level.ERROR, currentRecord, "Termed error:" + ex.getResponseBodyAsString()));
            errorCount++;
            rv = false;
        }
        return rv;
    }

    /**
     * Executes import operation. Reads incoming xml and process it
     * 
     * @param vocabularyId
     * @param ntrfDocument
     * @return
     */
    public String mapNtrfDocument(String jobtoken, UUID vocabularyId, VOCABULARY ntrfDocument, UUID userId) {
        logger.info("mapNtRfDocument: Vocabulary Id: {}", vocabularyId);

        Graph vocabulary;
        Long startTime = new Date().getTime();

        idMap.clear();
        reverseIdMap.clear();
        createdIdMap.clear();
        nconList.clear();
        bconList.clear();
        rconList.clear();

        // Get vocabulary
        try {
            vocabulary = termedService.getGraph(vocabularyId);
        } catch (NullPointerException nex) {
            logger.error("Vocabulary not found", nex);
            return "Vocabulary:<" + vocabularyId + "> not found";
        }

        if (!initImport(vocabularyId)) {
            return "Vocabulary:<" + vocabularyId + "> initialization error";
        }

        // Get statistic of terms
        List<?> l = ntrfDocument.getRECORDAndHEADAndDIAG();
        logger.info("Incoming objects count={}", l.size());

        // Get all reference-elements and build reference-url-map
        List<REFERENCES> externalReferences = l.stream()
                .filter(REFERENCES.class::isInstance)
                .map(o -> (REFERENCES) o)
                .collect(Collectors.toList());

        handleReferences(externalReferences, referenceMap);
        logger.info("Incoming reference count={}", externalReferences.size());

        // Get all records (mapped to terms) from incoming ntrf-document. Check object
        // type and typecast matching objects to list<>
        List<RECORD> records = l.stream()
                .filter(RECORD.class::isInstance)
                .map(o -> (RECORD) o)
                .collect(Collectors.toList());

        logger.info("Incoming records count={}", records.size());
        List<GenericNode> addNodeList = new ArrayList<>();
        List<Identifier> deleteNodeList = new ArrayList<>();

        ImportStatusResponse response = new ImportStatusResponse();
        response.setStatus(ImportStatus.PROCESSING);
        response.addStatusMessage(new ImportStatusMessage("Vocabulary", "Import started"));
        response.setProcessingTotal(records.size());
        response.setProcessingProgress(0);

        ytiMQService.setStatus(YtiMQService.STATUS_PROCESSING, jobtoken, userId.toString(), vocabulary.getUri(),
                response.toString());
        int flushCount = 0;
        int currentCount = 0;

        for (RECORD o : records) {
            currentRecord = o.getNumb();
            handleRECORD(vocabulary, o, addNodeList, deleteNodeList);
            flushCount++;
            currentCount++;
            response.setStatus(ImportStatus.PROCESSING);
            response.clearStatusMessages(); // Forget previous
            response.addStatusMessage(new ImportStatusMessage("Vocabulary", "Processing records"));
            response.setProcessingProgress(currentCount);
            response.setResultsError(errorCount);
            ytiMQService.setStatus(YtiMQService.STATUS_PROCESSING, jobtoken, userId.toString(), vocabulary.getUri(),
                    response.toString());
            // Flush datablock to the termed
            if (flushCount > 100) {
                flushCount = 0;
                GenericDeleteAndSave operation = new GenericDeleteAndSave(deleteNodeList, addNodeList);

                if (logger.isDebugEnabled()) {
                    logger.debug(JsonUtils.prettyPrintJsonAsString(operation));
                }

                response.setStatus(ImportStatus.PROCESSING);
                response.clearStatusMessages(); // Forget previous
                if (!updateAndDeleteInternalNodes(userId, operation, true)) {
                    response.addStatusMessage(new ImportStatusMessage("Vocabulary",
                            "Processing records, import failed for " + currentRecord));
                    errorCount++;
                } else {
                    response.addStatusMessage(new ImportStatusMessage("Vocabulary", "Processing records"));
                    // Import successfull, add id:s to resolved one.
                    addNodeList.forEach(node -> {
                        // Add id for reference resolving
                        createdIdMap.put(node.getCode(), node.getId());
                    });
                }
                response.setProcessingProgress(currentCount);
                ytiMQService.setStatus(YtiMQService.STATUS_PROCESSING, jobtoken, userId.toString(), vocabulary.getUri(),
                        response.toString());

                addNodeList.clear();
            }
        }
        GenericDeleteAndSave operation = new GenericDeleteAndSave(deleteNodeList, addNodeList);

        if (logger.isDebugEnabled()) {
            logger.debug(JsonUtils.prettyPrintJsonAsString(operation));
        }

        if (!updateAndDeleteInternalNodes(userId, operation, true)) {
            response.addStatusMessage(
                    new ImportStatusMessage("Vocabulary", "Processing records, import failed for " + currentRecord));
        } else {
            // Import successful, add id:s to resolved one.
            addNodeList.forEach(v -> {
                // Add id for reference resolving
                createdIdMap.put(v.getCode(), v.getId());
            });
        }

        List<GenericNode> addNodeListReferences = new ArrayList<>();

        // ReInitialize caches and after that, resolve rcon- and ncon-references
        idMap.clear();
        typeMap.clear();
        initImport(vocabularyId);
        // Just add reverse map
        idMap.forEach((k, v) -> reverseIdMap.put(v, k));

        handleLinks(userId, vocabulary);

        // Handle DIAG-elements and create collections from them
        List<DIAG> DIAGList = l.stream()
                .filter(DIAG.class::isInstance)
                .map(o -> (DIAG) o)
                .collect(Collectors.toList());

        logger.debug("DIAG-count={}", DIAGList.size());

        for (DIAG o : DIAGList) {
            handleDIAG(vocabulary, o, addNodeListReferences);
        }
        response.setStatus(ImportStatus.PROCESSING);
        response.addStatusMessage(new ImportStatusMessage("Vocabulary", "Processing DIAG number=" + DIAGList.size()));
        response.setProcessingProgress(records.size());
        ytiMQService.setStatus(YtiMQService.STATUS_PROCESSING, jobtoken, userId.toString(), vocabulary.getUri(),
                response.toString());
        // Add DIAG-list to vocabulary
        operation = new GenericDeleteAndSave(emptyList(), addNodeListReferences);

        if (logger.isDebugEnabled()) {
            logger.debug(JsonUtils.prettyPrintJsonAsString(operation));
        }

        if (!updateAndDeleteInternalNodes(userId, operation, true)) {
            logger.error("Diag termed error");
        }

        Long endTime = new Date().getTime();
        logger.info("Operation took {} s", (endTime - startTime) / 1000);
        logger.info("NTRF-imported {} concepts", records.size());
      
        response.clearStatusMessages();

        // Add all status lines as individual members before
        statusList.forEach(m -> {
            response.addStatusMessage(new ImportStatusMessage(m.getLevel(), m.getRecord(), m.getMessage().toString()));
            logger.info("Item: {} value: {}", m.getRecord(), m.getMessage());
        });

        response.setProcessingTotal(records.size());
        response.setProcessingProgress(records.size());
        response.setResultsWarning(statusList.size());
        response.setResultsError(errorCount);

        if (errorCount > 0) {
            response.setStatus(ImportStatus.FAILURE);

        } else if (!statusList.isEmpty()) {
            response.setStatus(ImportStatus.SUCCESS_WITH_ERRORS);
        } else {
            response.setStatus(ImportStatus.SUCCESS);
        }

        ytiMQService.setStatus(YtiMQService.STATUS_READY, jobtoken, userId.toString(), vocabulary.getUri(),
                response.toString());
        statusList.clear();
        return response.toString();
    }

    private void addConMap(Map<String, List<ConnRef>> conMap, String connType, UUID userId,
            Graph vocabulary) {
        List<GenericNode> addNodeList = new ArrayList<>();
        conMap.forEach((key, rlist) -> {

            // Resolve source id.
            UUID sourceId = createdIdMap.get(key);
            if (sourceId != null) {
                // Fetch node for update
                GenericNode gn = null;
                try {
                    gn = termedService.getConceptNode(vocabulary.getId(), sourceId);
                } catch (NullPointerException nex) {
                    logger.warn("Can't found concept node: {} id: {} in vocabulary {}", key, sourceId,
                            vocabulary.getId());
                }
                if (gn != null) {
                    Map<String, List<Identifier>> refMap = gn.getReferences();
                    List<Identifier> idref;

                    logger.debug("Add {} list to {} size: {}",connType, key, rlist.size());

                    // Iterate through list and add them to node
                    // BCON broader / isPartOf (generic/partitive) default is broader
                    // RCON no reftype-definition so always related
                    // NCON narrower/hasPart (generic/partitive) default is narrover
                    String refListName;
                    for (ConnRef ref : rlist) {
                        if (connType.equalsIgnoreCase("BCON")) {
                            // Default is generic -> broader
                            idref = refMap.get("broader");
                            refListName = "broader";
                            if (ref.getType() != null && ref.getType().equalsIgnoreCase("partitive")) {
                                idref = refMap.get("isPartOf");
                                refListName = "isPartOf";
                            }
                        } else if (connType.equalsIgnoreCase("NCON")) {
                            // default is generic->narrower
                            idref = refMap.get("narrower");
                            refListName = "narrower";
                            if (ref.getType() != null && ref.getType().equalsIgnoreCase("partitive")) {
                                idref = refMap.get("hasPart");
                                refListName = "hasPart";
                            }
                        } else {
                            // RCON
                            idref = refMap.get("related");
                            refListName = "related";
                        }

                        if (idref == null) {
                            idref = new ArrayList<>();
                        }
                        // Use name and resolve target id using it.
                        UUID refId = idMap.get(ref.getReferenceString());
                        if (refId != null) {
                            ref.setTargetId(refId);
                        } else {
                            logger.error("Can't resolve id for {}", ref.getReferenceString());
                        }

                        // @TODO! Go through refList and add only if missing.

                        if (!ref.getTargetId().equals(NULL_ID)) {
                            if (sourceId.equals(ref.getTargetId())) {
                                logger.error("Self-reference removed from {} id: {}", key, sourceId);
                                statusList.add(new StatusMessage(key,
                                        "Self-reference removed from " + key + " id:" + sourceId));
                            } else {
                                idref.add(new Identifier(ref.getTargetId(), typeMap.get("Concept").getDomain()));
                                // Put back int the correct list
                                refMap.put(refListName, idref);
                                logger.info("{} -> {} {}", refListName, ref.getReferenceString(), ref.getTargetId());
                            }
                        } else {
                            logger.error("Ref-target-id not found for :{}", ref.getCode());

                            statusList.add(new StatusMessage(currentRecord,
                                    connType + " Ref-target-id not found for : " + ref.getCode()));
                        }
                    }
                    // Add it back to termed
                    addNodeList.add(gn);
                } else {
                    logger.warn("Cant' resolve following! {} type: {} = {} -- vocab={}",key, connType, sourceId,
                            vocabulary.getId());
                    statusList.add(new StatusMessage(currentRecord, connType + " reference match failed. for " + key));
                }
            } else {
                logger.error("Can't find source id: {}", key);
            }
        });
        if (!addNodeList.isEmpty()) {
            // add (N/B/R)CON-changes as one big block

            GenericDeleteAndSave operation = new GenericDeleteAndSave(emptyList(), addNodeList);
            if (!updateAndDeleteInternalNodes(userId, operation, true)) {
                logger.error("CONN link adding: Termed error ");
            }
        }
        addNodeList.clear();
    }

    /**
     * After Concept and Term creation, add missing references
     * 
     * @param userId
     * @param vocabulary
     */
    private void handleLinks(UUID userId, Graph vocabulary) {
        addConMap(nconList, "NCON", userId, vocabulary);
        if (rconList.size() > 0) {
            addConMap(rconList, "RCON", userId, vocabulary);
        }
        if (bconList.size() > 0) {
            addConMap(bconList, "BCON", userId, vocabulary);
        }
    }

    /**
     * Initialize importer. - Read given vocabulary for meta-types, cache them -
     * Read all existing nodes and cache their URI/UUID-values
     * 
     * @param vocabularyId UUID of the vocabulary
     */
    private boolean initImport(UUID vocabularyId) {
        // Get metamodel types for given vocabulary
        List<MetaNode> metaTypes = termedService.getTypes(vocabularyId);
        metaTypes.forEach(t -> typeMap.put(t.getId(), t));

        // Create hashmap to store information between code/URI and UUID so that we can
        // update values upon same vocabulary
        try {
            List<GenericNode> nodeList = termedService.getNodes(vocabularyId);
            nodeList.forEach(o -> {
                logger.debug("Code: {}, UUID: {}, URI: {}", o.getCode(), o.getId(), o.getUri());
                if (o.getCode() != null && !o.getCode().isEmpty()) {
                    idMap.put(o.getCode(), o.getId());
                }
                if (o.getUri() != null && !o.getUri().isEmpty()) {
                    idMap.put(o.getUri(), o.getId());
                }
            });
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private void handleDIAG(Graph vocabularity, DIAG diag, List<GenericNode> addNodeList) {
        String code = diag.getNumb();
        logger.debug("DIAG Name={} Code={}", diag.getName(), code);

        UUID collectionId = idMap.get(code);
        // Generate new if not update
        if (collectionId == null) {
            collectionId = UUID.randomUUID();
        }
        // references list for colection member concepts
        Map<String, List<Identifier>> references = new HashMap<>();
        // Construct empty member list
        List<Identifier> memberRef = new ArrayList<>();
        // Construct properties map and add name as preflabel
        Map<String, List<Attribute>> properties = new HashMap<>();
        // Default lang is finnish at the time
        addProperty("prefLabel", properties, new Attribute("fi", diag.getName()));

        // get links and add them to references/member map

        List<LINK> l = diag.getLINK();
        l.forEach(li -> {
            String linkTarget = li.getHref();
            currentRecord = diag.getNumb();
            // Remove #
            if (linkTarget.startsWith("#")) {
                linkTarget = linkTarget.substring(1);
            }

            UUID targetUUID = idMap.get(linkTarget);
            if (targetUUID == null) {
                // try original
                targetUUID = idMap.get(li.getHref());
            }
            if (targetUUID != null && !targetUUID.equals(NULL_ID)) {
                memberRef.add(new Identifier(targetUUID, typeMap.get("Concept").getDomain()));
                references.put("member", memberRef);
            } else {
                String msg = String.format("DIAG: %s LINK-target %s <%s>  not added into the collection",
                        diag.getNumb(),
                        li.getHref(),
                        NtrfUtil.parseHrefText(li.getContent()));

                logger.warn(msg);
                statusList.add(new StatusMessage(currentRecord, msg));
            }
        });

        // Construct Node as Collection-type
        String uri = vocabularity.getUri().endsWith("/")
                ? vocabularity.getUri() + "/" + code
                : vocabularity.getUri() + code;
        GenericNode node = new GenericNode(collectionId, code, uri, 0L,
                userProvider.getUser().getUsername(), new Date(), "", new Date(), typeMap.get("Collection").getDomain(),
                properties, references, emptyMap());
        // Just add it
        addNodeList.add(node);
    }

    /**
     * TSK-NTRF packs external references as REFERENCES-items. Here we go through
     * them and cache values for later usage
     * 
     * @param referencesTypeList
     * @param refMap
     */
    private void handleReferences(List<REFERENCES> referencesTypeList,
            HashMap<String, HashMap<String, String>> refMap) {
        referencesTypeList.forEach(reftypes -> {
            List<REF> refs = reftypes.getREFOrREFHEAD().stream()
                    .filter(REF.class::isInstance)
                    .map(o -> (REF) o)
                    .collect(Collectors.toList());

            refs.forEach(r -> {
                // Filter all JAXBElements

                List<JAXBElement> elems;
                HashMap<String, String> fields = new HashMap<>();
                String name = r.getREFNAME();
                String url;
                String text;

                // Text is a structured component
                // In DTD this is a string or REMK.... <!ELEMENT REFTEXT (#PCDATA | REMK | B | I
                // | BR | LINK)*>
                REFTEXT rtx = r.getREFTEXT();
                elems = rtx.getContent().stream()
                        .filter(JAXBElement.class::isInstance)
                        .map(o -> (JAXBElement) o)
                        .collect(Collectors.toList());

                for (JAXBElement o : elems) {
                    if (o.getName().toString().equalsIgnoreCase("REFNAME")) {
                        name = o.getValue().toString();
                    }
                    if (o.getName().toString().equalsIgnoreCase("REFTEXT")) {
                        REFTEXT rt = (REFTEXT) o.getValue();
                        // Can be String | REMK | B | I | BR | LINK
                        // @TODO! add handling
                        text = rt.getContent().toString();
                        fields.put("text", text);
                    }
                    if (o.getName().toString().equalsIgnoreCase("REFLINK")) {
                        if (!o.getValue().toString().isEmpty()) {
                            url = o.getValue().toString();
                            fields.put("url", url);
                        }
                    }
                    logger.debug("Cache incoming external references: field={}", fields);
                }

                // add fields to referenceMap
                if (name != null) {
                    refMap.put(name, fields);
                }
            });
        });
    }

    /**
     * Remove orphan terms under given concept and graph
     */
    private void cleanReferences(UUID graphId, UUID conceptId, List<Identifier> deleteNodeList) {
        logger.info("clearTerm from: {} concept: {}", graphId, conceptId);
        // Get concept
        GenericNode node = termedService.getConceptNode(graphId, conceptId);
        // get references and delete terms and synonyms
        Map<String, List<Identifier>> references = node.getReferences();

        deleteNodeList.addAll(references.getOrDefault("prefLabelXl", new ArrayList<>()));
        deleteNodeList.addAll(references.getOrDefault("altLabelXl", new ArrayList<>()));
        deleteNodeList.addAll(references.getOrDefault("notRecommendedSynonym", new ArrayList<>()));
        deleteNodeList.addAll(references.getOrDefault("exactMatch", new ArrayList<>()));
        deleteNodeList.addAll(references.getOrDefault("closeMatch", new ArrayList<>()));
        deleteNodeList.addAll(references.getOrDefault("broadMatch", new ArrayList<>()));
        deleteNodeList.addAll(references.getOrDefault("relatedMatch", new ArrayList<>()));
        deleteNodeList.addAll(references.getOrDefault("narrowMatch", new ArrayList<>()));
    }

    /**
     * Handle mapping of RECORD. NTRF-models all concepts as RECORD-fields. It
     * contains actual terms and references to other concepts and external links.
     * See following example incomimg NTRF:
     * <RECORD numb="tmpOKSAID116" upda="Riina Kosunen, 2018-03-16">
     * <LANG value="fi"> <TE> <TERM>kasvatus</TERM> <SOURF>SRemes</SOURF> </TE>
     * <DEF>vuorovaikutukseen perustuva toiminta, jonka tavoitteena on kehittää
     * yksilöstä eettisesti vastuukykyinen yhteiskunnan jäsen<SOURF>wikipedia + rk +
     * pikkuryhma_01 + tr45 + tr49 + ssu + vaka_tr_01 + vaka_tr_02 +
     * tr63</SOURF></DEF> <NOTE>Kasvatuksen myötä kulttuuriset arvot, tavat ja
     * normit välittyvät ja muovautuvat. Osaltaan kasvatuksen tavoite on siirtää
     * kulttuuriperintöä sekä tärkeinä pidettyjä arvoja ja traditioita seuraavalle
     * sukupolvelle, mutta kasvatuksen avulla halutaan myös uudistaa ajattelu- ja
     * toimintatapoja. Kasvatuksen sivistystehtävänä on tietoisesti ohjata
     * yksilöllisen identiteetin muotoutumista ja huolehtia, että muotoutuminen
     * tapahtuu sosiaalisesti hyväksyttävällä tavalla.<SOURF>vaka_tr_02 +
     * tr63</SOURF></NOTE> <NOTE>Varhaiskasvatuksella tarkoitetaan
     * varhaiskasvatuslain (<LINK href=
     * "https://www.finlex.fi/fi/laki/ajantasa/1973/19730036">36/1973</LINK>) mukaan
     * lapsen suunnitelmallista ja tavoitteellista kasvatuksen,
     * <RCON href="#tmpOKSAID117">opetuksen (1)</RCON> ja hoidon muodostamaa
     * kokonaisuutta, jossa painottuu pedagogiikka.<SOURF>vk-peruste + ssu +
     * vaka_tr_02</SOURF></NOTE>
     * <NOTE><RCON href="#tmpOKSAID452">Perusopetuksella</RCON> on
     * opetustavoitteiden lisäksi kasvatustavoitteita. Perusopetuslain (<LINK href=
     * "https://www.finlex.fi/fi/laki/ajantasa/1998/19980628">628/1998</LINK>)
     * mukaan perusopetuksella pyritään kasvattamaan oppilaita ihmisyyteen ja
     * eettisesti vastuukykyiseen yhteiskunnan jäsenyyteen sekä antamaan heille
     * elämässä tarpeellisia tietoja ja taitoja.<SOURF>rk + pikkuryhma_01 +
     * tr45</SOURF></NOTE> <NOTE>Yliopistolain (<LINK href=
     * "https://www.finlex.fi/fi/laki/ajantasa/2009/20090558">558/2009</LINK>)
     * mukaan <RCON href="#tmpOKSAID162">yliopistojen</RCON> tehtävänä on edistää
     * vapaata tutkimusta sekä tieteellistä ja taiteellista sivistystä, antaa
     * tutkimukseen perustuvaa ylintä opetusta (1) sekä kasvattaa
     * <RCON href="#tmpOKSAID227">opiskelijoita</RCON> palvelemaan isänmaata ja
     * ihmiskuntaa.<SOURF>558/2009 + tr45</SOURF></NOTE> <NOTE>Englannin käsite
     * education on laajempi kuin suomen kasvatus, niin että termillä education
     * viitataan kasvatuksen lisäksi muun muassa
     * <RCON href="#tmpOKSAID117">opetukseen (1)</RCON>,
     * <RCON href="#tmpOKSAID121">koulutukseen (1)</RCON> ja sivistykseen.<SOURF>rk
     * + KatriSeppala</SOURF></NOTE> <NOTE>Käsitteen tunnus: tmpOKSAID116</NOTE>
     * </LANG> <LANG value="sv"> <TE> <TERM>fostran</TERM> <SOURF>36/1973_sv +
     * kielityoryhma_sv_04</SOURF> </TE> </LANG> <LANG value="en"> <TE>
     * <EQUI value="broader"></EQUI> <TERM>education</TERM> <HOGR>1</HOGR>
     * <SOURF>ophall_sanasto</SOURF> </TE> <SY> <TERM>upbringing</TERM>
     * <SOURF>MOT_englanti</SOURF> </SY> </LANG>
     * <BCON href="#tmpOKSAID122" typr="generic">koulutus (2)</BCON>
     * <NCON href="#tmpOKSAID123" typr="generic">koulutuksen toteutus</NCON>
     * <CLAS>yleinen/yhteinen</CLAS> <CHECK>hyväksytty</CHECK> </RECORD>
     * 
     * @param vocabulary Graph-node from termed. It contains base-uri where esck
     *                     Concept and Term is bound
     * @param r
     */
    void handleRECORD(Graph vocabulary, RECORD r, List<GenericNode> addNodeList, List<Identifier> deleteNodeList) {
        UUID currentId;
        String createdBy;
        LocalDate lastModifiedDate;
        // Attributes are stored to property-list
        Map<String, List<Attribute>> properties = new HashMap<>();
        // references synomyms and preferred tems and so on
        Map<String, List<Identifier>> references = new HashMap<>();

        String code = r.getNumb();
        // Check whether id exist and create id
        if (idMap.get(code) != null) {
            logger.debug("UPDATE operation {}", code);
            currentId = idMap.get(code);

            // Delete terms from existing concept before updating content
            cleanReferences(vocabulary.getId(), currentId, deleteNodeList);
        } else {
            logger.debug("CREATE NEW operation {}", code);
            currentId = UUID.randomUUID();
        }

        // Default creator is importing user
        createdBy = userProvider.getUser().getUsername();

        logger.info("Record id: {}", code);
        // Add info to editorial note
        String editorialNote = "";

        // Type can be 'vanhentunut', 'aputermi', 'ulottuvuus'
        if (Arrays.asList("aputermi", "ulottuvuus").contains(r.getType())) {
            editorialNote = r.getType();

            if (r.getType().equalsIgnoreCase("ulottuvuus")) {
                logger.warn("Dropping 'ulottuvuus' type node");
                statusList.add(new StatusMessage(currentRecord, "Dropping 'ulottuvuus' type record"));
                return;
            }
        } else if ("vanhentunut".equals(r.getType())) {
            addProperty("status", properties, new Attribute("", Status.RETIRED.name()));
        }

        if (r.getUpda() != null) {
            // Store that information to the modificationHistory
            String[] upd = r.getUpda().split(",");
            if (upd.length == 2) {
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                try {
                    lastModifiedDate = LocalDate.parse(upd[1].trim(), df);
                    editorialNote = editorialNote + " - Viimeksi muokattu, " + lastModifiedDate;
                } catch (DateTimeParseException dex) {
                    statusList.add(new StatusMessage(currentRecord, "Parse error for date" + dex.getMessage()));
                    logger.error("Parse error for date", dex);
                }
            }
        }
        if (!editorialNote.isEmpty()) {
            Attribute att = new Attribute("fi", editorialNote);
            addProperty("editorialNote", properties, att);
        }

        // Resolve terms and collect list of them for insert.
        List<GenericNode> terms = new ArrayList<>();
        // Needs to be final for lambda
        final UUID concept = currentId;
        // Filter LANG elements as list.
        List<LANG> langs = r.getLANG();
        // RECORD/LANG/TE/TERM -> prefLabel
        langs.forEach(o -> handleLANG(concept, terms, o, properties, references, vocabulary));

        // Handle Subject
        List<SUBJ> subjs = r.getSUBJ();
        subjs.forEach(o -> handleSUBJ(o, properties));

        // Filter CLAS elements as list
        List<CLAS> clas = r.getCLAS();
        // RECORD/CLAS ->
        clas.forEach(o -> handleCLAS(o, properties));

        // Filter CHECK elements as list
        if (r.getCHECK() != null && !r.getCHECK().isEmpty()) {
            // RECORD/CHECK ->
            handleCHECK(r.getCHECK(), r.getStat(), properties);
        }

        // Status (overrides CHECK)
        addStatusProperty(r.getStat(), properties);

        if (r.getREMK() != null) {
            r.getREMK().forEach(o -> handleREMK(o, properties, vocabulary));
        }
        // Filter BCON elements as list
        List<BCON> bcon = r.getBCON();
        for (BCON o : bcon) {
            // RECORD/BCON
            handleBCON(currentId, o);
        }

        List<RCON> rcon = r.getRCON();
        for (RCON o : rcon) {
            // RECORD/RCON
            handleRCON(currentId, o);
        }

        // Filter NCON elemets as list
        List<NCON> ncon = r.getNCON();
        for (NCON o : ncon) {
            handleNCON(currentId, o);
        }

        var conceptLinks = new ArrayList<GenericNode>();

        for (ECON econ : r.getECON()) {
            if (!Arrays.asList("exactMatch", "closeMatch").contains(econ.getTypr())) {
                logger.warn("Invalid type of concept link: {}", econ.getTypr());
                statusList.add(new StatusMessage(currentRecord, "Invalid reference type " + econ.getTypr()));
                continue;
            }
            handleExternalConcepts(references, conceptLinks, econ.getHref(), econ.getTypr());
        }

        for (RCONEXT rConExt : r.getRCONEXT()) {
            handleExternalConcepts(references, conceptLinks, rConExt.getHref(), "relatedMatch");
        }

        for (BCONEXT bConExt : r.getBCONEXT()) {
            handleExternalConcepts(references, conceptLinks, bConExt.getHref(), "broadMatch");
        }

        for (NCONEXT nConExt : r.getNCONEXT()) {
            handleExternalConcepts(references, conceptLinks, nConExt.getHref(), "narrowMatch");
        }

        TypeId typeId = typeMap.get("Concept").getDomain();
        GenericNode node = new GenericNode(currentId, code, vocabulary.getUri() + code, 0L, createdBy, new Date(), "", new Date(),
                typeId, properties, references, emptyMap());
        // Send item to termed-api
        // First add terms
        addNodeList.addAll(terms);
        // Add concept link nodes
        addNodeList.addAll(conceptLinks);
        // then concept itself
        addNodeList.add(node);
    }

    private void handleExternalConcepts(Map<String, List<Identifier>> references, ArrayList<GenericNode> conceptLinks,
                                        String uri, String refType) {
        var conceptLinkType = typeMap.get("ConceptLink").getDomain();
        var id = UUID.randomUUID();

        try {
            var resolvedResource = resolveService.resolveResource(uri);
            var conceptNode = termedService.getConcept(resolvedResource.getGraphId(), resolvedResource.getId());
            var terminology = termedService.getVocabulary(resolvedResource.getGraphId());

            var conceptLinkProperties = Map.of(
                    "prefLabel", getPrefLabel(conceptNode),
                    "targetId", List.of(new Attribute("", conceptNode.getId().toString())),
                    "targetGraph", List.of(new Attribute("", conceptNode.getType().getGraphId().toString())),
                    "vocabularyLabel", getPrefLabel(terminology)
            );

            conceptLinks.add(
                    new GenericNode(id, "concept-link-" + id, terminology.getUri() + "/concept-link-" + id,
                            0L, "", new Date(), "", new Date(), conceptLinkType,
                            conceptLinkProperties, emptyMap(), emptyMap())
            );
            var refs = references.getOrDefault(refType, new ArrayList<>());
            refs.add(new Identifier(id, conceptLinkType));
            references.put(refType, refs);
        } catch (Exception e) {
            logger.warn("Error handling external concepts {}, {}", uri, e.getMessage());
            statusList.add(new StatusMessage(currentRecord, "Related concept not found " + uri));
        }
    }

    private List<Attribute> getPrefLabel(GenericNodeInlined node) {
        List<Attribute> attributes;
        if (node.getType().getId().equals(NodeType.Concept)) {
            attributes = node.getReferences()
                    .get("prefLabelXl").get(0)
                    .getProperties().get("prefLabel");
        } else {
            attributes = node.getProperties().get("prefLabel");
        }

        return attributes.stream()
                .map(prop -> new Attribute(prop.getLang(), prop.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Handle mapping of individual Lang-element. This contains Terms which needs to
     * be created and definition and some other information which should be stored
     * under parent concept prefLabel value is found under LANG/TE/TERM
     * parent.definition is under LANG/TE/DEF parent.source-elements are found under
     * LANG/TE/SOURF, LANG/TE/DEF/SOURF and LANG/TE/NOTE/SOURF all of them are
     * mapped to same source-list Incoming NTRF: <LANG value="fi"> <TE>
     * <TERM>opetus</TERM> <HOGR>1</HOGR> <SOURF>harmon + tr45</SOURF> </TE>
     * <DEF>vuorovaikutukseen perustuva toiminta, jonka tavoitteena on
     * <RCON href="#tmpOKSAID118">oppiminen</RCON><SOURF>wikipedia + rk +
     * tr45</SOURF></DEF> <NOTE>Opetuksella (1) ja
     * <RCON href="#tmpOKSAID116">kasvatuksella</RCON> on osin yhteneväisiä
     * tavoitteita.<SOURF>vaka_tr_02 + tr63</SOURF></NOTE> <NOTE>Englannin käsite
     * education on laajempi kuin suomen opetus (1), niin että termillä education
     * viitataan opetuksen (1) lisäksi muun muassa
     * <RCON href="#tmpOKSAID116">kasvatukseen</RCON>,
     * <RCON href="#tmpOKSAID121">koulutukseen (1)</RCON> ja sivistykseen.<SOURF>rk
     * + KatriSeppala</SOURF></NOTE> <NOTE>Käsitteen tunnus: tmpOKSAID117</NOTE>
     * </LANG> <LANG value="en"> <TE> <EQUI value="broader"></EQUI>
     * <TERM>education</TERM> <HOGR>1</HOGR> <SOURF>ophall_sanasto</SOURF> </TE>
     * <SY> <TERM>upbringing</TERM> <SOURF>MOT_englanti</SOURF> </SY> </LANG>
     * 
     * @param o            LANGType containing incoming NTRF-block
     * @param vocabulary Graph-element containing information of parent
     *                     vocabulary like id and base-uri
     */
    private int handleLANG(UUID currentConcept, List<GenericNode> termsList, LANG o,
                           Map<String, List<Attribute>> parentProperties, Map<String, List<Identifier>> parentReferences,
                           Graph vocabulary) {
        // generate random UUID as a code and use it as part if the generated URI
        String code = UUID.randomUUID().toString();

        logger.debug("Handle LANG: {}", o.getValue());

        // Attributes are stored to property-list
        Map<String, List<Attribute>> properties = new HashMap<>();

        // Filter TE elemets as list and add mapped elements as properties under node
        if (o.getTE() != null) {
            // TE/TERM TE/TERM/GRAM
            // TE/SOURF
            // TE/REMK
            // TE/HOGR
            // TE/EQUI
            // TE/SCOPE
            // TE/ADD
            handleTE(o.getTE(), o.getValue().value(), // lang
                    properties, parentProperties, vocabulary);
        }

        // DEFINITION
        List<DEF> def = o.getDEF();
        // Definition is complex multi-line object which needs to be resolved
        for (DEF d : def) {
            handleDEF(currentConcept, d, o.getValue().value(), parentProperties, properties,
                    vocabulary);
        }
        // NOTE
        List<NOTE> notes = o.getNOTE();
        for (NOTE n : notes) {
            handleNOTE(currentConcept, n, o.getValue().value(), parentProperties, properties,
                    vocabulary);
        }

        Consumer<Termcontent> parentReferencesConsumer = obj -> {
            String propertyName;

            if (obj instanceof SY) {
                propertyName = "altLabelXl";
            } else if (obj instanceof STE) {
                propertyName = "searchTerm";
            } else {
                propertyName = "notRecommendedSynonym";
            }

            GenericNode n = handleSY(obj, o.getValue().value(), vocabulary);
            termsList.add(n);
            List<Identifier> ref;
            if (parentReferences.get(propertyName) != null) {
                ref = parentReferences.get(propertyName);
            } else {
                ref = new ArrayList<>();
            }
            ref.add(new Identifier(n.getId(), typeMap.get("Term").getDomain()));
            parentReferences.put(propertyName, ref);
        };

        // SY (synonym) is just like TE
        o.getSY().forEach(parentReferencesConsumer);

        // STE Search-terms
        o.getSTE().forEach(parentReferencesConsumer);

        // DTEA = notRecommendedSynonym term
        o.getDTEA().forEach(parentReferencesConsumer);

        // DTE = notRecommendedSynonym term
        o.getDTE().forEach(parentReferencesConsumer);

        // DTEB = retired term missing currently from META
        // handled now as DTEA
        o.getDTEB().forEach(parentReferencesConsumer);

        TypeId typeId = typeMap.get("Term").getDomain();
        // Uri is parent-uri/term-'code'
        GenericNode node;
        String uri = vocabulary.getUri().endsWith("/")
                ? vocabulary.getUri() + "term-" + code
                : vocabulary.getUri() + "/term-" + code;

        if (idMap.get(code) != null) {
            node = new GenericNode(idMap.get(code), code, uri, 0L, "", new Date(),
                    "", new Date(), typeId, properties, emptyMap(), emptyMap());
        } else {
            node = new GenericNode(code, uri, 0L, "", new Date(), "", new Date(),
                    typeId, properties, emptyMap(), emptyMap());
            // Set just created term as preferred term for concept

            List<Identifier> ref;
            if (parentReferences.get("prefLabelXl") != null)
                ref = parentReferences.get("prefLabelXl");
            else
                ref = new ArrayList<>();
            ref.add(new Identifier(node.getId(), typeId));
            parentReferences.put("prefLabelXl", ref);
        }
        termsList.add(node);
        // Add id for reference resolving
        createdIdMap.put(node.getCode(), node.getId());
        return termsList.size();
    }

    private void handleTERM(TERM term, String lang, Map<String, List<Attribute>> properties) {
        logger.debug("Handle Term: {}", term);

        String termName = "";
        List<Object> content = term.getContent();
        for (Object li : content) {
            if (li instanceof String) {
                termName = termName.concat(li.toString().trim() + " ");
            } else if (li instanceof GRAM) {
                handleGRAM((GRAM) li, properties);
                String prefLabel = ((GRAM) li).getContent();
                // Add actual pref-label for term
                logger.info("Handle Term with GRAM: {}", prefLabel);
                termName = termName.concat(prefLabel+" ");
            } else {
                logger.error("TERM: unhandled contentclass={} value={}", li.getClass().getName(), li);
            }
        }
        if (!termName.isEmpty()) {
            Attribute att = new Attribute(lang, termName.trim());
            addProperty("prefLabel", properties, att);
        }
    }

    private void handleTE(TE te, String lang, Map<String, List<Attribute>> properties,
                          Map<String, List<Attribute>> parentProperties, Graph vocabularity) {
        logger.debug("Handle Te: {}", te);
        // If GEOG used
        if (te.getGEOG() != null) {
            lang = (lang.toLowerCase() + "-" + te.getGEOG().toUpperCase());
        }
        // LANG/TE/TERM
        if (te.getTERM() != null) {
            handleTERM(te.getTERM(), lang, properties);
        }

        // LANG/TE/SOURF
        if (te.getSOURF() != null) {
            handleSOURF(te.getSOURF(), null, parentProperties, vocabularity);
        }
        // LANG/TE/HOGR
        if (te.getHOGR() != null && !te.getHOGR().isEmpty()) {
            Attribute att = new Attribute(null, te.getHOGR());
            addProperty("termHomographNumber", properties, att);
        }

        // LANG/TE/SCOPE
        if (te.getSCOPE() != null) {
            handleSCOPE(te.getSCOPE(), properties);
        }
        // LANG/TE/EQUI
        if (te.getEQUI() != null) {
            handleEQUI(te.getEQUI(), properties);
        }
        // LANG/TE/REMK
        if (te.getREMK() != null) {
            handleREMK(te.getREMK(), properties, vocabularity);
        }
        if (te.getADD() != null) {
            handleADD(te.getADD(), properties);
        }

        addStatusProperty(te.getStat(), properties);
    }

    private void handleREMK(REMK remk, Map<String, List<Attribute>> properties, Graph vocabulary) {

        List<?> content = remk.getContent();
        String editorialNote = "";
        for (Object o : content) {
            if (o instanceof String) {
                editorialNote = editorialNote + o;
                editorialNote = NtrfUtil.escapeStringContent(editorialNote);
            } else if (o instanceof JAXBElement) {
                JAXBElement elem = (JAXBElement) o;
                editorialNote = editorialNote + elem.getValue().toString();
            } else if (o instanceof LINK) {
                LINK l = (LINK) o;
                String linkRef = NtrfUtil.parseLinkRef(l, vocabulary);
                editorialNote = editorialNote
                        .concat("<a href='" + l.getHref() + "' data-type='external'>" + linkRef + "</a>");
            } else if (o instanceof SOURF) {
                handleSOURF((SOURF) o, null, properties, vocabulary);
            } else {
                statusList.add(new StatusMessage(currentRecord,
                        " REMK: unhandled contentclass=" + o.getClass().getName() + " value=" + o));
                logger.error("REMK: unhandled contentclass={} value={}", o.getClass().getName(), o);
            }
        }

        if (!editorialNote.isEmpty()) {
            Attribute att = new Attribute("fi", editorialNote);
            addProperty("editorialNote", properties, att);
        }
    }

    private void handleEQUI(EQUI equi, Map<String, List<Attribute>> properties) {
        // Attribute string value = broader | narrower | near-equivalent
        String eqvalue = "=";
        if (equi.getValue().equalsIgnoreCase("broader"))
            eqvalue = ">";
        if (equi.getValue().equalsIgnoreCase("narrower"))
            eqvalue = "<";
        if (equi.getValue().equalsIgnoreCase("near-equivalent"))
            eqvalue = "~";
        Attribute att = new Attribute(null, eqvalue);
        addProperty("termEquivalency", properties, att);
    }

    private void handleADD(String add, Map<String, List<Attribute>> properties) {
        Attribute att = new Attribute(null, add);
        addProperty("termInfo", properties, att);
    }

    private void handleSCOPE(SCOPE scope, Map<String, List<Attribute>> properties) {
        logger.debug("HandleScope = {}", scope.getContent());
        scope.getContent().forEach(li -> {
            if (li instanceof String) {
                Attribute att = new Attribute(null, li.toString());
                addProperty("scope", properties, att);
            } else if (li instanceof LINK) {
                // <SCOPE>yliopistolain <LINK
                // href="https://www.finlex.fi/fi/laki/kaannokset/2009/en20090558_20160644.pdf">558/2009
                // käännöksessä</LINK></SCOPE>
                logger.info("Unimplemented SCOPE WITH LINK");
                // @TODO! Make impl
            }
        });

    }

    private void handleGRAM(GRAM gt, Map<String, List<Attribute>> properties) {
        // termConjugation (single, plural)
        if (gt.getValue() != null && gt.getValue().equalsIgnoreCase("pl")) {
            // Currently not localized
            Attribute att = new Attribute("fi", "monikko");
            addProperty("termConjugation", properties, att);
        } else if (gt.getValue() != null && gt.getValue().equalsIgnoreCase("n pl")) {
            // Currently not localized plural and neutral
            Attribute att = new Attribute("fi", "monikko");
            addProperty("termConjugation", properties, att);
            att = new Attribute("fi", "neutri");
            addProperty("termFamily", properties, att);
        } else if (gt.getValue() != null && gt.getValue().equalsIgnoreCase("f pl")) {
            // Currently not localized plural and neutral
            Attribute att = new Attribute("fi", "monikko");
            addProperty("termConjugation", properties, att);
            att = new Attribute("fi", "feminiini");
            addProperty("termFamily", properties, att);
        }
        // termFamily
        if (gt.getGend() != null && gt.getGend().equalsIgnoreCase("f")) {
            // feminiini
            // Currently not localized
            Attribute att = new Attribute("fi", "feminiini");
            addProperty("termFamily", properties, att);
        } else if (gt.getGend() != null && gt.getGend() != null && gt.getGend().equalsIgnoreCase("m")) {
            // maskuliiini
            Attribute att = new Attribute("fi", "maskuliini");
            addProperty("termFamily", properties, att);
        } else if (gt.getGend() != null && gt.getGend().equalsIgnoreCase("n")) {
            // Neutri
            Attribute att = new Attribute("fi", "neutri");
            addProperty("termFamily", properties, att);
        }
        // wordClass
        if (gt.getPos() != null && !gt.getPos().isEmpty()) {
            // Currently not localized, just copy wordClass as such
            Attribute att = new Attribute(null, gt.getPos());
            addProperty("wordClass", properties, att);
        }
    }

    /**
     * Handle CHECK->status-property mapping
     * 
     * @param o          CHECK-field
     * @param properties Propertylist where status is added
     */
    private Attribute handleCHECK(String o, String stat, Map<String, List<Attribute>> properties) {
        String status = "DRAFT";
        /*
         * keskeneräinen | 'INCOMPLETE' korvattu | 'SUPERSEDED' odottaa hyväksyntää |
         * 'SUBMITTED' | 'RETIRED' | 'INVALID' hyväksytty | 'VALID' | 'SUGGESTED'
         * luonnos | 'DRAFT'
         */
        if (o.equalsIgnoreCase("hyväksytty"))
            status = "DRAFT";

        if (stat != null && stat.equalsIgnoreCase("vanhentunut"))
            status = "RETIRED";
        Attribute att = new Attribute(null, status);
        addProperty("status", properties, att);
        return att;
    }

    private void handleSUBJ(SUBJ subj, Map<String, List<Attribute>> properties) {
        if (subj != null) {
            subj.getContent().forEach(o -> {
                if (o instanceof String) {
                    Attribute att = new Attribute(null, o.toString());
                    addProperty("conceptScope", properties, att);
                } else {
                    logger.error("SUBJ unknown instance type: {}", o.getClass().getName());
                    statusList.add(
                            new StatusMessage(currentRecord, "SUBJS unknown instance type:" + o.getClass().getName()));
                }
            });
        }
    }

    /**
     * NTRF Broader-concept parsing Can be direct hierarchical or partitive
     * reference <BCON href="#tmpOKSAID122" typr="generic">koulutus (2)</BCON>
     * <BCON href="#tmpOKSAID148" typr="partitive">toimipisteen</BCON>
     * 
     * @param o
     */
    private void handleBCON(UUID currentConcept, BCON o) {
        logger.debug("handleBCON: {}", o.getHref());

        String brefId = o.getHref();
        // Remove #
        if (brefId.startsWith("#")) {
            brefId = o.getHref().substring(1);
        }

        logger.info("handleBCON add item from source record: {} --> target: {} Type {}",
                currentRecord, brefId, o.getTypr());
        ConnRef conRef = new ConnRef();
        // Use delayed resolving, so save record id for logging purposes
        conRef.setCode(currentRecord);
        conRef.setReferenceString(brefId);
        // Null id, as a placeholder for target
        conRef.setId(currentConcept);
        conRef.setType(o.getTypr());
        conRef.setTargetId(NULL_ID);

        // if not yet defined, create list and populate it
        List<ConnRef> reflist;
        if (bconList.containsKey(currentRecord)) {
            reflist = bconList.get(currentRecord);
        } else {
            reflist = new ArrayList<>();
        }
        reflist.add(conRef);
        bconList.put(currentRecord, reflist);
    }

    /**
     * NTRF Related-concept parsing Can be direct hierarchical or partitive
     * reference <RCON href="#tmpOKSAID122">koulutus (2)</RCON>
     * 
     * @param o
     */
    private void handleRCON(UUID currentConcept, RCON o) {
        logger.debug("handleRCON: {}", o.getHref());
        String brefId = o.getHref();
        // Remove #
        if (brefId.startsWith("#")) {
            brefId = o.getHref().substring(1);
        }

        logger.info("handleRCON add item from source record: {} --> target: {}", currentRecord, brefId);
        ConnRef conRef = new ConnRef();
        // Use delayed resolving, so save record id for logging purposes
        conRef.setCode(currentRecord);
        conRef.setReferenceString(brefId);
        // Null id, as a placeholder for target
        conRef.setId(currentConcept);
        conRef.setTargetId(NULL_ID);

        // if not yet defined, create list and populate it
        List<ConnRef> reflist;
        if (rconList.containsKey(currentRecord)) {
            reflist = rconList.get(currentRecord);
        } else {
            reflist = new ArrayList<>();
        }
        reflist.add(conRef);
        rconList.put(currentRecord, reflist);
    }

    /**
     * NTRF Related-concept parsing Can be direct hierarchical or partitive
     * reference <RCON href="#tmpOKSAID564">ylioppilastutkintoa</RCON>
     * <RCON href="#tmpOKSAID436">Eurooppa-koulujen</RCON>
     * <RCON href="#tmpOKSAID456">lukiokoulutuksen</RCON>*
     * 
     * @param rc
     */
    private void handleRCONRef(UUID currentConcept, RCON rc) {
        logger.debug("handleRCON ref: {}", rc.getHref());
        String rrefId = rc.getHref();
        // Remove #
        if (rrefId.startsWith("#"))
            rrefId = rc.getHref().substring(1);

        logger.info("handleRCONRef add item from source record: {} --> target: {}", currentRecord, rrefId);
        ConnRef conRef = new ConnRef();
        // Use delayed resolving, so save record id for logging purposes
        conRef.setCode(currentRecord);
        conRef.setReferenceString(rrefId);
        // Null id, as a placeholder for target
        conRef.setId(currentConcept);
        conRef.setType(rc.getTypr());
        conRef.setTargetId(NULL_ID);

        // if not yet defined, create list and populate it
        List<ConnRef> reflist;
        if (rconList.containsKey(currentRecord)) {
            reflist = rconList.get(currentRecord);
        } else {
            reflist = new ArrayList<>();
        }
        reflist.add(conRef);
        rconList.put(currentRecord, reflist);
    }

    /**
     * NTRF Related-concept parsing Can be direct hierarchical or partitive
     * reference <BCON href="#tmpOKSAID564">ylioppilastutkintoa</RCON>
     * <BCON href="#tmpOKSAID436">Eurooppa-koulujen</RCON>
     * <BCON href="#tmpOKSAID456">lukiokoulutuksen</RCON>*
     * 
     */
    private void handleBCONRef(UUID currentConcept, BCON bc) {
        logger.debug("handleBCON ref: {}", bc.getHref());
        String rrefId = bc.getHref();
        // Remove #
        if (rrefId.startsWith("#")) {
            rrefId = bc.getHref().substring(1);
        }

        logger.info("handleBCONRef add item from source record: {} --> target: {}", currentRecord, rrefId);
        ConnRef conRef = new ConnRef();
        // Use delayed resolving, so save record id for logging purposes
        conRef.setCode(currentRecord);
        conRef.setReferenceString(rrefId);
        // Null id, as a placeholder for target
        conRef.setId(currentConcept);
        conRef.setType(bc.getTypr());
        conRef.setTargetId(NULL_ID);

        // if not yet defined, create list and populate it
        List<ConnRef> reflist;
        if (bconList.containsKey(currentRecord)) {
            reflist = bconList.get(currentRecord);
        } else {
            reflist = new ArrayList<>();
        }
        reflist.add(conRef);
        bconList.put(currentRecord, reflist);
    }

    /**
     * NTRF Related-concept parsing Can be direct hierarchical or partitive
     * reference <NCON href="#tmpOKSAID564">ylioppilastutkintoa</NCON>
     * <NCON href="#tmpOKSAID436">Eurooppa-koulujen</NCON>
     * <NCON href="#tmpOKSAID456">lukiokoulutuksen</NCON>*
     *
     */
    private void handleNCONRef(UUID currentConcept, NCON nc) {
        logger.debug("handleNCON ref: {}", nc.getHref());
        String rrefId = nc.getHref();
        // Remove #
        if (rrefId.startsWith("#")) {
            rrefId = nc.getHref().substring(1);
        }

        logger.info("handleNCONRef add item from source record: {} --> target: {}", currentRecord, rrefId);
        ConnRef conRef = new ConnRef();
        // Use delayed resolving, so save record id for logging purposes
        conRef.setCode(currentRecord);
        conRef.setReferenceString(rrefId);
        // Null id, as a placeholder for target
        conRef.setId(currentConcept);
        conRef.setType(nc.getTypr());
        conRef.setTargetId(NULL_ID);

        // if not yet defined, create list and populate it
        List<ConnRef> reflist;
        if (nconList.containsKey(currentRecord)) {
            reflist = nconList.get(currentRecord);
        } else {
            reflist = new ArrayList<>();
        }
        reflist.add(conRef);
        nconList.put(currentRecord, reflist);
    }

    /**
     * Actual NCON reference in body part, so this time it is added
     * 
     * @param o
     */
    private void handleNCON(UUID currentConcept, NCON o) {
        logger.debug("handleNCON: {}", o.getHref());
        String nrefId = o.getHref();
        if (nrefId != null) {
            // Remove #
            if (nrefId.startsWith("#")) {
                nrefId = o.getHref().substring(1);
            }
            logger.info("handleNCON add item from source record: {} --> target: {}", currentRecord, nrefId);
            ConnRef conRef = new ConnRef();
            // Use delayed resolving, so save record id for logging purposes
            conRef.setCode(currentRecord);
            conRef.setReferenceString(nrefId);
            // Null id, as a placeholder for target
            conRef.setId(currentConcept);
            conRef.setType(o.getTypr());
            conRef.setTargetId(NULL_ID);

            // if not yet defined, create list and populate it
            List<ConnRef> reflist;
            if (nconList.containsKey(currentRecord)) {
                reflist = nconList.get(currentRecord);
            } else {
                reflist = new ArrayList<>();
            }
            reflist.add(conRef);
            nconList.put(currentRecord, reflist);
        }
    }

    /**
     * Set up ConceptClass with CLAS-element data.
     * 
     * @param o          CLAS object containing String list
     * @param properties
     */
    private void handleCLAS(CLAS o, Map<String, List<Attribute>> properties) {
        if (!o.getContent().isEmpty()) {
            List<String> clasList = new ArrayList<>();
            o.getContent().forEach(obj -> {
                clasList.add(obj.toString());
            });
            Attribute att = new Attribute(null, clasList.toString().substring(1, clasList.toString().length() - 1));
            addProperty("conceptClass", properties, att);
        } else {
            logger.warn("Empty CLAS element.");
        }
    }

    private Attribute handleDEF(UUID currentConcept, DEF def, String lang,
            Map<String, List<Attribute>> parentProperties,
            Map<String, List<Attribute>> termProperties, Graph vocabulary) {

        String defString = getContentWithLinks(def.getContent(), currentConcept,
                termProperties, vocabulary, lang);
        logger.debug("Definition={}", defString);

        // Add definition if exists.
        if (!defString.isEmpty()) {
            Attribute att = new Attribute(lang, defString);
            addProperty("definition", parentProperties, att);
            return att;
        }
        return null;
    }

    private String getContentWithLinks(List<Object> content, UUID currentConcept,
                                              Map<String, List<Attribute>> termProperties,
                                              Graph vocabulary, String lang) {
        String result = "";
        for (Object element : content) {
            if (element instanceof String) {
                result = result.concat(getStringContent(result, (String) element));
            } else if (element instanceof RCON) {
                RCON rc = (RCON) element;
                result = result.concat(
                        NtrfUtil.getLink(vocabulary.getUri(), rc.getHref(), rc.getContent(), "related"));
                // Add also reference
                handleRCONRef(currentConcept, rc);
            } else if (element instanceof BCON) {
                BCON bc = (BCON) element;
                result = result.concat(
                        NtrfUtil.getLink(vocabulary.getUri(), bc.getHref(), bc.getContent(), getLinkTypr(bc)));
                // Add also reference
                handleBCONRef(currentConcept, bc);
            } else if (element instanceof NCON) {
                NCON nc = (NCON) element;
                result = result.concat(
                        NtrfUtil.getLink(vocabulary.getUri(), nc.getHref(), nc.getContent(), getLinkTypr(nc)));
                // Add also reference
                handleNCONRef(currentConcept, nc);
            } else if (element instanceof ECON) {
                ECON econ = (ECON) element;
                result = result.concat(
                        NtrfUtil.getLink(econ.getHref(), "", econ.getContent(), null));
            } else if (element instanceof RCONEXT) {
                RCONEXT rconext = (RCONEXT) element;
                result = result.concat(
                        NtrfUtil.getLink(rconext.getHref(), "", rconext.getContent(), null));
            } else if (element instanceof BCONEXT) {
                BCONEXT bconext = (BCONEXT) element;
                result = result.concat(
                        NtrfUtil.getLink(bconext.getHref(), "", bconext.getContent(), null));
            } else if (element instanceof NCONEXT) {
                NCONEXT nconext = (NCONEXT) element;
                result = result.concat(
                        NtrfUtil.getLink(nconext.getHref(), "", nconext.getContent(), null));
            } else if (element instanceof SOURF) {
                handleSOURF((SOURF) element, null, termProperties, vocabulary);
                // Add refs as sources-part.
                updateSources(((SOURF) element).getContent(), lang, termProperties);
            } else if (element instanceof REMK) {
                handleREMK((REMK) element, termProperties, vocabulary);
            } else if (element instanceof LINK) {
                LINK lc = (LINK) element;
                if (lc.getContent() != null && !lc.getContent().isEmpty()) {
                    String linkRef = NtrfUtil.parseLinkRef(lc, vocabulary);
                    if (linkRef.startsWith("href:")) {
                        linkRef = linkRef.substring(5);
                    }
                    result = result.concat("<a href='" + linkRef + "' data-type='external'>"
                            + NtrfUtil.escapeStringContent(lc.getContent().get(0).toString().trim()) + "</a> ");
                }
            } else if (element instanceof JAXBElement) {
                // HOGR
                JAXBElement el = (JAXBElement) element;
                if (el.getName().toString().equalsIgnoreCase("HOGR")) {
                    result = result.trim() + " (" + el.getValue().toString() + ")";
                }
            } else {
                logger.error("DEF, unhandled CLASS={}", element.getClass().getName());
                statusList.add(new StatusMessage(currentRecord, "DEF, unhandled CLASS=" + element.getClass().getName()));
            }
        }

        return result
                .replaceAll(" , ", ", ")
                .replaceAll(" \\. ", ". ")
                .replaceAll("( )+", " ")
                .trim();
    }

    private static String getLinkTypr(Object obj) {
        if (obj instanceof BCON) {
            return "partitive".equalsIgnoreCase(((BCON)obj).getTypr()) ? "isPartOf" : "broader";
        } else if (obj instanceof NCON) {
            return "partitive".equalsIgnoreCase(((NCON)obj).getTypr()) ? "hasPart" : "narrower";
        }
        return null;
    }

    @NotNull
    private static String getStringContent(String content, String strElement) {
        strElement = NtrfUtil.escapeStringContent(strElement).trim();

        // Add space after and before if not the first content or element is not empty
        if (content.isEmpty()) {
            return strElement + " ";
        } else if (strElement.isEmpty() && !content.endsWith(" ")) {
            return " ";
        } else if (!strElement.isEmpty()) {
            return " " + strElement + " ";
        }
        return "";
    }

    private void handleNOTE(UUID currentConcept, NOTE note, String lang,
            Map<String, List<Attribute>> parentProperties,
            Map<String, List<Attribute>> termProperties, Graph vocabulary) {

        String noteString = getContentWithLinks(note.getContent(), currentConcept, termProperties, vocabulary, lang);

        // Add note if exists.
        if (!noteString.isEmpty()) {
            logger.debug("handleNote() Adding note: {}", noteString);
            Attribute att = new Attribute(lang, noteString);
            addProperty("note", parentProperties, att);
        }
    }

    /**
     * Sample of incoming synonyms <SY>
     * <TERM>examensarbete<GRAM gend="n"></GRAM></TERM> <SCOPE>akademisk</SCOPE>
     * <SOURF>fisv_utbild_ordlista + kielityoryhma_sv</SOURF> </SY> <SY>
     * <EQUI value="near-equivalent"></EQUI> <TERM>vetenskapligt
     * arbete<GRAM gend="n"></GRAM></TERM> <SCOPE>akademisk</SCOPE>
     * <SOURF>fisv_utbild_ordlista + kielityoryhma_sv</SOURF> </SY>
     */

    private GenericNode handleSY(Termcontent synonym, String lang, Graph vocabularity) {
        logger.debug("handleSY-part: {}", synonym.toString());
        // Synonym fields
        String equi;
        // Attributes are stored to property-list
        Map<String, List<Attribute>> properties = new HashMap<>();
        if (synonym.getGEOG() != null) {
            lang = (lang.toLowerCase() + "-" + synonym.getGEOG().toUpperCase());
        }

        if (synonym.getEQUI() != null) {
            // Attribute string value = broader | narrower | near-equivalent
            EQUI eqt = synonym.getEQUI();
            equi = eqt.getValue();
            String eqvalue = "=";
            if (equi.equalsIgnoreCase("broader"))
                eqvalue = ">";
            if (equi.equalsIgnoreCase("narrower"))
                eqvalue = "<";
            if (equi.equalsIgnoreCase("near-equivalent"))
                eqvalue = "~";

            Attribute att = new Attribute(null, eqvalue);
            addProperty("termEquivalency", properties, att);
        }
        if (synonym.getHOGR() != null) {
            Attribute att = new Attribute(null, synonym.getHOGR());
            addProperty("termHomographNumber", properties, att);
        }
        if (synonym.getSCOPE() != null) {
            SCOPE sc = synonym.getSCOPE();
            sc.getContent().forEach(o -> {
                if (o instanceof String) {
                    Attribute att = new Attribute(null, o.toString());
                    addProperty("scope", properties, att);
                } else {
                    logger.error("SCOPE unknown instance type: {}", o.getClass().getName());
                    statusList.add(
                            new StatusMessage(currentRecord, "SCOPE unknown instance type:" + o.getClass().getName()));
                }
            });
        }
        if (synonym.getSOURF() != null) {
            handleSOURF(synonym.getSOURF(), null, properties, vocabularity);
        }
        if (synonym.getTERM() != null) {
            handleTERM(synonym.getTERM(), lang, properties);
        }
        if (synonym.getADD() != null) {
            handleADD(synonym.getADD(), properties);
        }

        if (synonym instanceof SY) {
            addStatusProperty(((SY)synonym).getStat(), properties);
        } else if (synonym instanceof STE) {
            addStatusProperty(((STE)synonym).getStat(), properties);
        } else if (synonym instanceof DTE) {
            addStatusProperty(((DTE)synonym).getStat(), properties);
        } else if (synonym instanceof DTEA) {
            addStatusProperty(((DTEA)synonym).getStat(), properties);
        } else if (synonym instanceof DTEB) {
            addStatusProperty(((DTEB)synonym).getStat(), properties);
        }

        // create new synonym node (Term)
        TypeId typeId = typeMap.get("Term").getDomain();
        // Uri is parent-uri/term-'code'
        UUID id = UUID.randomUUID();
        String code = "term-"+id.toString();

        String uri = vocabularity.getUri().endsWith("/")
                ? vocabularity.getUri() + code
                : vocabularity.getUri() + "/" + code;

        GenericNode node = new GenericNode(id, code, uri, 0L, "", new Date(), "", new Date(),
                typeId, properties, emptyMap(), emptyMap());
        // Add id for reference resolving
        createdIdMap.put(node.getCode(), node.getId());
        return node;
    }

    /**
     * From
     * 
     * @param source
     * @param lang
     * @param properties
     * @param vocabularity
     * @return
     */
    private void handleSOURF(SOURF source, String lang, Map<String, List<Attribute>> properties,
            Graph vocabularity) {
        logger.debug("handleSOURF-part {}", source.getContent());

        String sourceString = "";

        // TODO: SOURF should not contain only LINK elements or plain text
        List<?> sourceItems = source.getContent();
        for (Object se : sourceItems) {
            if (se instanceof String) {
                sourceString = sourceString.concat(se.toString());
                sourceString = NtrfUtil.escapeStringContent(sourceString);
            }
            else if (se instanceof NCON) {
                NCON rc = (NCON) se;
                sourceString = sourceString.concat("<a href='" + vocabularity.getUri());
                if (rc.getTypr() != null && !rc.getTypr().isEmpty()) {
                    sourceString = sourceString.concat(" data-typr ='" + rc.getTypr() + "'");
                }
                sourceString = sourceString.concat(">" + rc.getContent().toString() + "</a>");
            } else if (se instanceof BCON) {
                BCON bc = (BCON) se;
                sourceString = sourceString.concat("<a href='" + vocabularity.getUri());
                if (bc.getTypr() != null && !bc.getTypr().isEmpty()) {
                    sourceString = sourceString.concat(" data-typr ='" + bc.getTypr() + "'");
                }
                sourceString = sourceString.concat(">" + bc.getContent().toString() + "</a>");

            } else if (se instanceof RCON) {
                RCON rc = (RCON) se;
                sourceString = sourceString.concat("<a href='" + vocabularity.getUri());
                if (rc.getTypr() != null && !rc.getTypr().isEmpty()) {
                    sourceString = sourceString.concat(" data-typr ='" + rc.getTypr() + "'");
                }
                sourceString = sourceString.concat(">" + rc.getContent().toString() + "</a>");
            }
            else {
                if (se instanceof JAXBElement) {
                    JAXBElement j = (JAXBElement) se;
                    if (j.getName().toString().equalsIgnoreCase("RCON")) {
                        RCON rc = (RCON) j.getValue();
                        sourceString = sourceString.concat("<a href='" + vocabularity.getUri());
                        if (rc.getTypr() != null && !rc.getTypr().isEmpty()) {
                            sourceString = sourceString.concat(" data-typr ='" + rc.getTypr() + "'");
                        }
                        sourceString = sourceString.concat(">" + rc.getContent().get(0) + "</a>");
                    } else if (j.getName().toString().equalsIgnoreCase("SOURF")) {
                        SOURF sf = (SOURF) j.getValue();
                        if (sf.getContent() != null && !sf.getContent().isEmpty()) {
                            sourceString = sourceString.concat(" " + sf.getContent());
                            // Add refs as string and construct lines four sources-part.
                            updateSources(sf.getContent(), lang, properties);
                        }
                    } else {
                        logger.error("  UNKNOWN  SOURF-class {}", se.getClass().getName());
                        statusList.add(new StatusMessage(currentRecord,
                                "SOURF unknown instance type:" + se.getClass().getName()));
                    }
                }
            }
        }

        // Add definition if exists.
        if (!sourceString.isEmpty()) {
            Attribute att = new Attribute(lang, sourceString);
            addProperty("source", properties, att);
        }
    }

    /**
     * Add individual source-elements to the source-list for each individual
     * reference enumerated inside imported SOURF
     * 
     * @param srefs
     * @param lang
     * @param properties
     */
    private void updateSources(List<Object> srefs, String lang, Map<String, List<Attribute>> properties) {
        for (Object o : srefs) {
            updateSources(o.toString(), lang, properties);
        }
    }

    /**
     * Add individual source-elements from give string
     * 
     * @param srefs
     * @param lang
     * @param properties
     */
    private void updateSources(String srefs, String lang, Map<String, List<Attribute>> properties) {
        String[] fields = srefs.split("\\+");
        for (String s : fields) {
            s = s.trim();
            String sourcesString = "[" + s + "]";
            Map<String, String> m = referenceMap.get(s);
            if (m != null) {
                if (m.get("text") != null && !m.get("text").isEmpty()) {
                    sourcesString = sourcesString.concat("\n " + m.get("text") + "\n");
                }
                if (m.get("url") != null && !m.get("url").isEmpty()) {
                    sourcesString = sourcesString.concat(m.get("url"));
                }
            } else {
                logger.warn("Not matching reference found for: {}", s);
                statusList.add(new StatusMessage(currentRecord, "Not matching reference found for :" + s));
            }
            if (!sourcesString.isEmpty()) {
                logger.debug("ADDING sourf: {}", sourcesString);
                Attribute satt = new Attribute(lang, sourcesString);
                addProperty("source", properties, satt);
            }
        }
    }

    /**
     * Add individual named attribute to property list
     * 
     * @param attributeName like prefLabel
     * @param properties    Propertylist where attribute is added
     * @param att           Attribute to be added
     */
    private void addProperty(String attributeName, Map<String, List<Attribute>> properties, Attribute att) {
        if (!properties.containsKey(attributeName)) {
            List<Attribute> a = new ArrayList<>();
            a.add(att);
            properties.put(attributeName, a);
        } else {
            properties.get(attributeName).add(att);
        }
    }

    private void addStatusProperty(String value, Map<String, List<Attribute>> properties) {
        Status status = Status.DRAFT;
        if (value != null) {
            try {
                status = Status.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status provided {}", value);
            }
        }
        addProperty("status", properties, new Attribute("", status.name()));
    }
    /**
     * Can be used in with BCON, NCON and RCON references
     */
    private class ConnRef {
        String code;
        String referenceString;
        String type;
        UUID id;
        UUID targetId;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getReferenceString() {
            return referenceString;
        }

        public void setReferenceString(String referenceString) {
            this.referenceString = referenceString;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public void setTargetId(UUID targetId) {
            this.targetId = targetId;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    private class StatusMessage {
        Level level;
        String record;
        List<String> message = new ArrayList<>();

        public StatusMessage(String record, String msg) {
            this.level = Level.WARNING;
            this.record = record;
            this.message.add(msg);
        }

        public StatusMessage(Level level, String record, String msg) {
            this.level = level;
            this.record = record;
            this.message.add(msg);
        }

        public ImportStatusMessage.Level getLevel() {
            return level;
        }

        public void setLevel(Level level) {
            this.level = level;
        }

        public String getRecord() {
            return record;
        }

        public void setRecord(String record) {
            this.record = record;
        }

        public List<String> getMessage() {
            return message;
        }

        public void setMessage(List<String> message) {
            this.message = message;
        }

    }

}
