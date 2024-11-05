package fi.vm.yti.terminology.api.v2.service;

import fi.vm.yti.terminology.api.v2.exception.ExcelParseException;
import fi.vm.yti.terminology.api.v2.exception.ImportException;
import fi.vm.yti.terminology.api.v2.mapper.ExcelMapper;
import fi.vm.yti.terminology.api.v2.mapper.NTRFMapper;
import fi.vm.yti.terminology.api.v2.ntrf.VOCABULARY;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.security.TerminologyAuthorizationManager;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class ImportService {

    private final TerminologyRepository terminologyRepository;

    private final TerminologyAuthorizationManager authorizationManager;

    private final IndexService indexService;

    public ImportService(TerminologyRepository terminologyRepository,
                         TerminologyAuthorizationManager authorizationManager,
                         IndexService indexService) {
        this.terminologyRepository = terminologyRepository;
        this.authorizationManager = authorizationManager;
        this.indexService = indexService;
    }

    public void importNTRF(String prefix, MultipartFile file) {

        var model = terminologyRepository.fetchByPrefix(prefix);
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        try {
            JAXBContext context = JAXBContext.newInstance(VOCABULARY.class);
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(file.getInputStream());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            VOCABULARY voc = (VOCABULARY) unmarshaller.unmarshal(reader);

            NTRFMapper.mapTerminology(voc, model, authorizationManager.getUser());

            terminologyRepository.put(model.getGraphURI(), model);
            indexService.reindexTerminology(model);

        } catch (JAXBException | IOException | XMLStreamException e) {
            throw new ImportException(e.getMessage());
        }
    }

    public void importExcel(String prefix, MultipartFile file) {
        var model = terminologyRepository.fetchByPrefix(prefix);
        check(authorizationManager.hasRightsToTerminology(prefix, model));

        try {
            ExcelMapper.mapSimpleExcel(model, file.getInputStream(), authorizationManager.getUser());
            terminologyRepository.put(model.getGraphURI(), model);
            indexService.reindexTerminology(model);
        } catch (ExcelParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ImportException(e.getMessage());
        }
    }
}
