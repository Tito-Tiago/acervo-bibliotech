package com.bibliotech.bibliotech.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gerenciamento de Documentos - OpenDoc")
                        .version("1.0")
                        .description("Documentação da API para manipulação de documentos ODF usando jOpenDocument.")
                        .termsOfService("swagger.io")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
