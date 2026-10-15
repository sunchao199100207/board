/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.config.WebConfig
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.web.servlet.HandlerInterceptor
 *  org.springframework.web.servlet.config.annotation.CorsRegistry
 *  org.springframework.web.servlet.config.annotation.InterceptorRegistry
 *  org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 */
package cn.game.board.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig
implements WebMvcConfigurer {
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(new String[]{"*"}).allowedMethods(new String[]{"GET", "POST"});
    }

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor((HandlerInterceptor)new /* Unavailable Anonymous Inner Class!! */).addPathPatterns(new String[]{"/**"});
    }
}

