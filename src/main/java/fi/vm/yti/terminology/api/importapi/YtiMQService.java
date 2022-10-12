package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Service;

import javax.jms.*;

import java.util.*;

@Service
@EnableJms
public class YtiMQService {

    private final AuthenticatedUserProvider userProvider;

    private final String subSystem;

    private static final Logger logger = LoggerFactory.getLogger(YtiMQService.class);

    public static final String[] SET_VALUES = new String[] { "Vocabulary", "Test", "CodeList", "VocabularyExcel" };
    public static final Set<String> SUPPORTED_SYSTEMS = new HashSet<>(Arrays.asList(SET_VALUES));

    // JMS-client
    private JmsMessagingTemplate jmsMessagingTemplate;
    
    // Define public status values
    public final static int STATUS_PREPROCESSING = 1;
    public final static int STATUS_PROCESSING = 2;
    public final static int STATUS_READY = 3;
    public final static int STATUS_FAILED = 4;

    private Map<String, Message> currentStatus = new HashMap<>();

    private JmsTemplate jmsTemplate;

    @Autowired
    public YtiMQService(AuthenticatedUserProvider userProvider,
                        JmsMessagingTemplate jmsMessagingTemplate,
                        JmsTemplate jmsTemplate,
                        @Value("${mq.active.subsystem}") String subSystem) {
        this.userProvider = userProvider;
        this.jmsMessagingTemplate = jmsMessagingTemplate;
        this.jmsTemplate = jmsTemplate;
        this.subSystem = subSystem;
    }

    public HttpStatus getStatus(UUID jobtoken){

        logger.info("IncomingQueue!");
        browseQueue(subSystem+"Incoming");
        logger.info("StatusQueue!");
        browseQueue(subSystem+"Status");
        logger.info("ProcessingQueue!");
        browseQueue(subSystem+"Processing");
        logger.info("ReadyQueue!");
        browseQueue(subSystem+"Ready");
        browseQueue(subSystem + "ExcelImport");
        // Status not_found/running/errors
        Message mess = currentStatus.get(jobtoken.toString());
        if (mess != null) {
            int status = (int)mess.getHeaders().get("status");
            switch(status){
                case YtiMQService.STATUS_READY:{
                    logger.debug("Import done for {}", jobtoken);
                    return HttpStatus.OK;
                }
                case YtiMQService.STATUS_PROCESSING:{
                    logger.debug("Processing {}", jobtoken);
                    long expirationtime=System.currentTimeMillis() - (long)mess.getHeaders().get("timestamp");
                    if( expirationtime > 60 * 1000) {
                        return HttpStatus.OK;
                    } else
                        return HttpStatus.PROCESSING;
                }
                case YtiMQService.STATUS_PREPROCESSING:{
                    logger.warn("Import operation already started for "+jobtoken);
                    return  HttpStatus.NOT_ACCEPTABLE;
                }
                case YtiMQService.STATUS_FAILED: {
                    logger.error("Import failed {}", jobtoken);
                    return HttpStatus.INTERNAL_SERVER_ERROR;
                }
            }
        }

        // Query status information from ActiveMQ
        if(getJobState(jobtoken, subSystem+"Ready")){
            logger.debug("Import done for {}", jobtoken);
            return HttpStatus.OK;
        } else if (getJobState(jobtoken, subSystem+"Incoming")){
            logger.warn("Import operation already started for {}", jobtoken);
            return  HttpStatus.NOT_ACCEPTABLE;
        } else if (getJobState(jobtoken, subSystem+"Status")){
            logger.debug("Processing {}", jobtoken);
            return HttpStatus.PROCESSING;
        } else if (getJobState(jobtoken, subSystem+"Processing")){
            logger.debug("Processing {}", jobtoken);
            return HttpStatus.PROCESSING;
        }
        return HttpStatus.NO_CONTENT;
    }

    public HttpStatus getStatus(UUID jobtoken, StringBuffer payload){
        // Status not_found/running/errors
        // Query status information from ActiveMQ
        browseQueue("VocabularyStatus");
        Message mess = currentStatus.get(jobtoken.toString());
        if (mess != null) {
            // return also payload
            payload.append(mess.getPayload());
            int status = (int)mess.getHeaders().get("status");
            switch (status){
                case YtiMQService.STATUS_READY: {
                    logger.debug("Import done for {}", jobtoken);
                    return HttpStatus.OK;
                }
                case YtiMQService.STATUS_PROCESSING: {
                    logger.debug("Processing {}", jobtoken);
                    logger.debug("Timestamp={}", mess.getHeaders().get("timestamp"));
                    long expirationtime = System.currentTimeMillis() - (long)mess.getHeaders().get("timestamp");
                    logger.debug("current_time-stamp={}", expirationtime);
                    if( expirationtime > 10 * 60 * 1000) {
                        return HttpStatus.OK;
                    } else {
                        return HttpStatus.PROCESSING;
                    }
                }
                case YtiMQService.STATUS_PREPROCESSING: {
                    logger.warn("Import operation already started for {}", jobtoken);
                    return HttpStatus.NOT_ACCEPTABLE;
                }
                case YtiMQService.STATUS_FAILED: {
                    return HttpStatus.INTERNAL_SERVER_ERROR;
                }
            }
        }

        logger.debug("Processing {}", jobtoken);

        if (getJobState(jobtoken, subSystem+"Ready")) {
            logger.debug("Import done for {}", jobtoken);
            payload.append(getJobPayload(jobtoken, subSystem+"Ready"));
            logger.debug("Payload={}", payload);
            return HttpStatus.OK;
        } else if (getJobState(jobtoken, subSystem+"Incoming")) {
            logger.warn("Import operation already started for "+jobtoken);
            return  HttpStatus.NOT_ACCEPTABLE;
        } else if (getJobState(jobtoken, subSystem+"Status")) {
            payload.append(getJobPayload(jobtoken, subSystem+"Status"));
            logger.debug("Status Payload={}", payload);
            return HttpStatus.PROCESSING;
        } else if (getJobState(jobtoken, subSystem+"Processing")){
            payload.append(getJobPayload(jobtoken,subSystem+"Processing"));
            logger.debug("Processing Payload="+payload);
            return HttpStatus.PROCESSING;
        }
        return  HttpStatus.NO_CONTENT;
    }

    private boolean getJobState(UUID jobtoken,String queueName) {
        return
            jmsTemplate.browseSelected(queueName, "jobtoken='"+jobtoken.toString()+"'", new BrowserCallback<Boolean>() {
                @Override
                public Boolean doInJms(Session session, QueueBrowser browser) throws JMSException {
                    Enumeration messages = browser.getEnumeration();
                    return  messages.hasMoreElements();
                }
        });
    }


    public boolean checkIfImportIsRunning(String uri) {
        Boolean rv = false;
        // Check cached status first running if not ready
        Message mess = currentStatus.get(uri);
        if (mess != null){
            int status = (int)mess.getHeaders().get("status");
            logger.debug("YtiMQService checkIfImportIsRunning using cached state:{} \n {}", status, mess);
            if(status == YtiMQService.STATUS_PROCESSING || status == YtiMQService.STATUS_PREPROCESSING) {
                logger.debug("Timestamp="+(long)mess.getHeaders().get("timestamp"));
                long expirationtime=System.currentTimeMillis() - (long)mess.getHeaders().get("timestamp");
                logger.debug("current_time-stamp="+expirationtime);
                rv =true;
                if (expirationtime > 60 * 1000) {
                    logger.info("Status Expired for job:"+(String)mess.getHeaders().get("jobtoken"));
                    // cached item expired, clean it
                    currentStatus.remove(uri);
                    currentStatus.remove((String)mess.getHeaders().get("jobtoken"));
                    rv = false;
                }
            }
            // Cache found, use it
            return rv;
        }
        else {
            logger.debug("Not cached item found for "+uri);
        }

        // Check queues
        if (checkUriStatus(uri, subSystem+"Processing")) {
            rv = true;
        }
        return rv;
    }

    public boolean checkUriStatus(String uri, String queueName) {
        // selector uri='url' AND
        return jmsTemplate.browseSelected(queueName, "uri='"+uri+"'",new BrowserCallback<Boolean>() {
            @Override
            public Boolean doInJms(Session session, QueueBrowser browser) throws JMSException {
                Enumeration messages = browser.getEnumeration();
                logger.debug("checkUriStatus "+uri+" Queue:"+queueName+" status="+messages.hasMoreElements());
                return  messages.hasMoreElements();
            }
        });
    }

    private List<String> getJobJMSId(UUID jobtoken,String queueName) {
        String messageSelector = "jobtoken='"+jobtoken.toString()+"'";
        logger.debug("MessageSelector for browse: {} ", messageSelector);
        return jmsTemplate.browseSelected(queueName, messageSelector,new BrowserCallback<List<String>>() {
            @Override
            public List<String> doInJms(Session session, QueueBrowser browser) throws JMSException {
                List<String> rv = new ArrayList<>();
                Enumeration messages = browser.getEnumeration();
                if(messages.hasMoreElements()){
                    javax.jms.Message m = (javax.jms.Message)messages.nextElement();
                    rv.add(m.getJMSMessageID());
                    logger.debug("{} adding {}", m.getStringProperty("jobtoken"), m.getJMSMessageID());
                }
                return  rv;
            }
        });
    }

    public String browseQueue(String queue) {
        return jmsMessagingTemplate.getJmsTemplate().browse(queue, (session, browser) -> {
            Enumeration<?> messages = browser.getEnumeration();
            int total = 0;
            while (messages.hasMoreElements()) {
                javax.jms.Message m = (javax.jms.Message)messages.nextElement();
                if (m instanceof TextMessage) {
                    TextMessage message = (TextMessage) m;
                    logger.info(" browse message = {}", message);
                }
                total++;
            }
            return String.format("Total '%d elements waiting in %s", total, queue);
        });
    }

    private String getJobPayload(UUID jobtoken,String queueName) {
        String messageSelector = "jobtoken='"+jobtoken.toString()+"'";
        logger.debug(" getJobPayload MessageSelector for browse:"+messageSelector + " queue="+queueName);
        return jmsTemplate.browseSelected(queueName, messageSelector, new BrowserCallback<String>() {
            @Override
            public String doInJms(Session session, QueueBrowser browser) throws JMSException {
                String rv= null;
                Enumeration messages = browser.getEnumeration();
                if(messages.hasMoreElements()){
                    javax.jms.Message m = (javax.jms.Message)messages.nextElement();
                    if (m instanceof TextMessage) {
                        TextMessage message = (TextMessage) m;
                        rv = message.getText();
                    }
                    logger.debug("getPayload={}", rv);
                }
                return  rv;
            }
        });
    }

    public boolean deleteJmsStatusMessage(String jobtoken){
        boolean rv = false;
        ConnectionFactory cf = jmsTemplate.getConnectionFactory();

        if(getStatus(UUID.fromString(jobtoken)) != HttpStatus.NO_CONTENT) {
            Connection connection = null;
            Session session = null;
            try {
                connection = cf.createConnection();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue(subSystem + "Status");
                String selector = "jobtoken = '" + jobtoken + "'";
                javax.jms.Message message = jmsTemplate.receiveSelected(destination, selector);
                if (message != null) {
                    logger.debug(" delete message with selector: {} message={}", selector, message);
                    rv = true;
                }

            } catch (JMSException e) {
                logger.error("Failed to read message from MessageConsumer. " + e);
            } finally {
                try {
                    session.close();
                } catch (Exception e) { /* NOP */ }
                try {
                    connection.close();
                } catch (Exception e) { /* NOP */ }
            }
        } else {
            logger.debug("Can't find status message to delete");
        }
        return rv;
    }

    public int handleImportAsync(UUID jobtoken, MessageHeaderAccessor headers, String subsystem, String uri, String payload) {

        logger.debug("handleImportAsync subsystem: {} Uri: {}", subsystem, uri);
        if (!SUPPORTED_SYSTEMS.contains(subsystem)) {
            logger.error("Unsupported subsystem:<{}> (Currently supported subsystems: {})", subsystem, SET_VALUES);
            return  HttpStatus.NOT_ACCEPTABLE.value();
        }

        // If jobtoken is not set, create new one
        if(jobtoken == null) {
            jobtoken=UUID.randomUUID();
        }
        MessageHeaderAccessor accessor = getMessageHeaderAccessor(headers, jobtoken, subsystem, uri);

        Message mess = MessageBuilder
                    .withPayload(payload)
                    .setHeaders(accessor)
                    .build();

        // send item for processing
        logger.debug("Send job: {} to the processing queue: {}Incoming", jobtoken, subsystem);
        jmsMessagingTemplate.send(subsystem+"Incoming", mess);

        StatusTest(mess);        
        return  HttpStatus.OK.value();
    }

    @SendTo("${mq.active.subsystem}StatusTest")
    public Message StatusTest(Message mess){
        return mess;
    }

    private javax.jms.Message getMessageByJobtoken(UUID jobtoken, String queueName) {
        return jmsTemplate.browseSelected(queueName, "jobtoken='"+jobtoken.toString()+"'", new BrowserCallback<javax.jms.Message>() {
            @Override
            public javax.jms.Message doInJms(Session session, QueueBrowser browser) throws JMSException {
                javax.jms.Message mess = null;
                Enumeration messages = browser.getEnumeration();
                logger.debug("getMessagesByJobToken {}", messages.hasMoreElements());
                if(messages.hasMoreElements()){
                    mess = (javax.jms.Message)messages.nextElement();
                }
                return  mess;
            }
        });
    }

    @SendTo("${mq.active.subsystem}Status")
    public Message setStatus(int status, String jobtoken, String userId, String uri,  String payload) {

        // Add application specific headers
        MessageHeaderAccessor accessor = new MessageHeaderAccessor();
        // Authenticated user
        accessor.setHeader("userId",  userId);
        // Token which is used when querying status
        accessor.setHeader("jobtoken", jobtoken.toString());
        //  Use  jobtoken as correlation id
        accessor.setHeader("JMSCorrelationID",jobtoken.toString());
        // Target identification data
        accessor.setHeader("system", subSystem);
        accessor.setHeader("uri", uri);
        // Set status as int
        accessor.setHeader("status",status);

        Message mess = MessageBuilder
                .withPayload(payload)
                .setHeaders(accessor)
                .build();
        // send new  item to Status-queue
        if(mess != null) {
            logger.debug("Send status message to queue:");
            jmsMessagingTemplate.send(subSystem + "Status", mess);
            jmsTemplate.convertAndSend(subSystem + "Status", mess);
            // Update internal cache
            currentStatus.put(jobtoken, mess);
            currentStatus.put(uri, mess);

            logger.debug("SEND STATUS: {}", mess);
        }
        logger.debug("Send Status to QUEUE {}", mess);

        return mess;
    }

    /**
     * Send batches to queue
     *
     * @See fi.vm.yti.terminology.api.importapi.ExcelImportJmsListener
     */
    public void handleExcelImportAsync(UUID jobToken, MessageHeaderAccessor headers,
                                      String uri, List<List<GenericNode>> batches) {

        MessageHeaderAccessor accessor = getMessageHeaderAccessor(headers, jobToken, subSystem, uri);

        // Set initial status
        ImportStatusResponse response = new ImportStatusResponse();
        response.setStatus(ImportStatusResponse.ImportStatus.PROCESSING);
        response.setProcessingProgress(0);
        response.setProcessingTotal(batches.size());

        setStatus(STATUS_PROCESSING, jobToken.toString(),
                accessor.getHeader("userId").toString(),
                accessor.getHeader("uri").toString(),
                response.toString());

        int count = 1;

        for(List<GenericNode> batch : batches) {
            accessor.setHeader("currentBatch", count++);
            accessor.setHeader("totalBatchCount", batches.size());

            Message<List<GenericNode>> message = MessageBuilder
                    .withPayload(batch)
                    .setHeaders(accessor)
                    .build();
            jmsMessagingTemplate.send(subSystem + "ExcelImport", message);
        }
    }

    private MessageHeaderAccessor getMessageHeaderAccessor(MessageHeaderAccessor headers, UUID jobToken, String subsystem, String uri) {
        MessageHeaderAccessor accessor = new MessageHeaderAccessor();
        if (headers != null) {
            // Add application specific headers
            accessor.copyHeaders(headers.toMap());
        }
        // Authenticated user
        accessor.setHeader("userId", userProvider.getUser().getId().toString());
        // Token which is used when querying status
        accessor.setHeader("jobtoken", jobToken.toString());
        // Target identification data
        accessor.setHeader("system", subsystem);
        accessor.setHeader("uri", uri);
        // Use jobtoken as correlationID
        accessor.setHeader("JMSCorrelationID", jobToken.toString());
        return accessor;
    }
}
