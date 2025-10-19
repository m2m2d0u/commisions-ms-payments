package com.payment.commission.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8086}")
    private String serverPort;

    @Bean
    public OpenAPI commissionServiceOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Commission Service - Development");

        Contact contact = new Contact();
        contact.setName("Payment System Team");
        contact.setEmail("support@payment.com");

        License license = new License()
                .name("Proprietary")
                .url("https://payment.com/license");

        Info info = new Info()
                .title("Payment Commission Service API")
                .version("1.0.0")
                .description("BCEAO-compliant commission fee calculation and revenue tracking service")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
