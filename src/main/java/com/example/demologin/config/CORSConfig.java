package com.example.demologin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class CORSConfig implements WebMvcConfigurer {

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String corsAllowedOrigins;

    @Value("${CORS_ALLOW_LOCAL_DEV:true}")
    private boolean corsAllowLocalDev;

    private List<String> resolveOriginPatterns() {
        Set<String> originPatterns = new LinkedHashSet<>();

        // Keep stable frontend origins available by default.
        originPatterns.add("https://swp391-ckitchen.up.railway.app");
        originPatterns.add("https://feswp391ckitchen.vercel.app");

        if (corsAllowLocalDev) {
            originPatterns.add("http://localhost:3000");
            originPatterns.add("https://localhost:3000");
            originPatterns.add("http://127.0.0.1:3000");
        }

        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .forEach(originPatterns::add);
        }

        return new ArrayList<>(originPatterns);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> originPatterns = resolveOriginPatterns();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setExposedHeaders(List.of(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Methods",
                "Access-Control-Allow-Headers",
                "Authorization"
        ));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> originPatterns = resolveOriginPatterns();

        registry.addMapping("/**")
                .allowedOriginPatterns(originPatterns.toArray(new String[0]))
                .allowedHeaders("*")
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Methods","Access-Control-Allow-Headers", "Authorization")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}

