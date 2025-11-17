package com.example.oauth2prac.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {

        // API 정보 설정
        Info info = new Info()
                .title("Gyeol API Document")
                .version("v1.0.0")
                .description("Gyeol 프로젝트 API 명세서");

        // SecurityScheme 이름
        String jwtSchemeName = "jwtAuth";

        // API 요청 헤더에 인증 정보 포함
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);

        // SecuritySchemes 설정
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        // Cloud Run은 HTTPS만 지원하므로 자동으로 HTTPS 서버 설정
        Server prodServer = new Server()
                .url("https://gyeol-backend-k2urhc7ojq-du.a.run.app")
                .description("Production Server (Cloud Run)");

        Server devServer = new Server()
                .url("http://localhost:8080")
                .description("Development Server (Local)");

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components)
                .servers(Arrays.asList(prodServer, devServer));
    }
}