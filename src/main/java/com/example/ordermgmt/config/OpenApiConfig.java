package com.example.ordermgmt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import io.swagger.v3.oas.models.tags.Tag;
import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management API")
                        .version("1.0")
                        .description("API documentation for the Order Management System")
                        .contact(new Contact()
                                .name("Support")
                                .email("support@example.com")))
                .tags(Arrays.asList(
                        new Tag().name("0. Authentication")
                                .description("Everything you need to sign up, log in, and stay secure"),
                        new Tag().name("1. Customer Profile")
                                .description("Manage your personal account information and settings"),
                        new Tag().name("2. My Orders").description(
                                "Everything you need to place new orders, view your history, and manage existing purchases"),
                        new Tag().name("3. Product Catalog")
                                .description("Browse our full range of available products and prices"),
                        new Tag().name("4. Order Management (Admin)")
                                .description("Tools for administrators to oversee and manage all customer orders"),
                        new Tag().name("5. Pricing Control (Admin)").description(
                                "Adjust product prices, view historical price changes, and manage promotions"),
                        new Tag().name("6. Stock & Inventory (Admin)").description(
                                "Check product availability, update quantity in hand, and manage the warehouse"),
                        new Tag().name("7. Analytics & Reports (Admin)")
                                .description("View sales trends, monthly performance, and send reports to email")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}
