package com.gorani_samjichang.art_critique.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MainConfig implements WebMvcConfigurer {
    @Value("${front.server.host}")
    String frontHost;

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        // 모든 경로에 대해 모든 HTTP 메서드에 대한 CORS를 허용
        registry.addMapping("/**")
//                .allowedOrigins("http://localhost:9100")
                .allowedOrigins(frontHost, "http://localhost:9100", "http://artcritique.kro.kr")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true)
                .allowedHeaders("*");
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}