package fi.vm.yti.terminology.api.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AjpNioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class AjpConfig {

    private static final Logger logger = LoggerFactory.getLogger(AjpConfig.class);

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer(@Value("${tomcat.ajp.port:}") Integer ajpPort) {
        return server -> {
            if (ajpPort != null && server instanceof TomcatServletWebServerFactory) {
                logger.info("Modifying TomcatServletWebServerFactory to enable AJP at port " + ajpPort);
                try {
                    server.addAdditionalTomcatConnectors(ajpConnector(ajpPort));
                } catch (UnknownHostException e) {
                    logger.warn("Unable to configure AJP connector");
                }
            }
        };
    }

/*
    // NOTE: This is probably worse as it replaces instead of augments Spring Boot defaults.
    @Bean
    public ServletWebServerFactory servletContainer(@Value("${tomcat.ajp.port:}") Integer ajpPort) {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        if (ajpPort != null) {
            factory.addAdditionalTomcatConnectors(ajpConnector(ajpPort));
        }
        return factory;
    }
*/

    private Connector ajpConnector(int ajpPort) throws UnknownHostException {
        Connector ajpConnector = new Connector("AJP/1.3");
        ajpConnector.setPort(ajpPort);
        ajpConnector.setSecure(false);
        ajpConnector.setAllowTrace(false);
        ajpConnector.setScheme("http");
        ajpConnector.setProperty("allowedRequestAttributesPattern", ".*");

        AjpNioProtocol protocol= (AjpNioProtocol)ajpConnector.getProtocolHandler();
        protocol.setSecretRequired(false);
        protocol.setAddress(InetAddress.getByName("0.0.0.0"));

        return ajpConnector;
    }
}
