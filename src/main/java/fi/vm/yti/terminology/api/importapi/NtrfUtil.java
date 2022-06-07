package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.terminology.api.index.Vocabulary;
import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class NtrfUtil {

    public static VOCABULARY unmarshallXmlDocument(String message) throws JAXBException, XMLStreamException {
        JAXBContext jc = JAXBContext.newInstance(VOCABULARY.class);
        // Disable DOCTYPE-directive from incoming file.
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        // Unmarshall XMl with JAXB
        Reader inReader = new StringReader(message);
        XMLStreamReader xsr = xif.createXMLStreamReader(inReader);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        // At last, resolve ntrf-POJO's
        return (VOCABULARY) unmarshaller.unmarshal(xsr);
    }
}
