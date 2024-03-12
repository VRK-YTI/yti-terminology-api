package fi.vm.yti.terminology.api.v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "YTI Terminology Service",
        description = "YTI Terminology Service - Terminology API",
        termsOfService = "https://opensource.org/licenses/EUPL-1.2",
        contact = @Contact(name = "YTI Terminology Service by the Digital and Population Data Services Agency", url = "https://sanastot.suomi.fi/", email = "yhteentoimivuus@dvv.fi"),
        license = @License(name = "EUPL-1.2", url = "https://opensource.org/licenses/EUPL-1.2")
    ),
    servers = { @Server(url = "/terminology-api", description = "Terminology API Service") }
)
@SpringBootApplication
@ComponentScan(basePackages = "fi.vm.yti")
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
