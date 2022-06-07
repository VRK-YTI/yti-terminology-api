package fi.vm.yti.terminology.api.importapi;

import java.io.Reader;
import java.io.StringReader;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Session;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;

@Component
public class ImportJmsListener {

    private static final Logger logger = LoggerFactory.getLogger(ImportJmsListener.class);

    private final NtrfMapper ntrfMapper;

    public ImportJmsListener(NtrfMapper ntrfMapper) {
        this.ntrfMapper = ntrfMapper;
    }

    /**
     * Implements actual import operation.
     * @param message
     * @param session
     * @param jobtoken
     * @param userId
     * @param vocabularyId
     * @return
     * @throws JMSException
     */
    @JmsListener(id="NtrfProcessor", destination = "${mq.active.subsystem}Processing")
	@SendTo("${mq.active.subsystem}Ready")
	public Message<String> processMessage(final Message<String> message,
                                          Session session,
                                          @Header String jobtoken,
                                          @Header String userId,
                                          @Header String vocabularyId) throws JMSException {
	    // Consume incoming
        logger.info("Process message {}, session {}, user id {}", message.getHeaders(), session, userId);

        String payload = "{}";

        // Process ntrf item
        try {
            VOCABULARY voc = NtrfUtil.unmarshallXmlDocument(message.getPayload());
            payload = ntrfMapper.mapNtrfDocument(jobtoken, UUID.fromString(vocabularyId), voc, UUID.fromString(userId));
        } catch (XMLStreamException se) {
            logger.error(se.getMessage(), se);
        } catch(JAXBException je) {
            logger.error(je.getMessage(), je);
        }

        logger.info("Import handled: {}", payload);

        MessageHeaderAccessor accessor = new MessageHeaderAccessor();
        accessor.copyHeaders(message.getHeaders());

        // Set result as a payload and move it to ready-queue1
        Message<String> mess = MessageBuilder
                .withPayload(payload)
                .setHeaders(accessor)
                .build();
        return mess;
	}
}