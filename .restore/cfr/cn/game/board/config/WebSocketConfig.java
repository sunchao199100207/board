/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.config.WebSocketConfig
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.messaging.simp.config.MessageBrokerRegistry
 *  org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
 *  org.springframework.web.socket.config.annotation.StompEndpointRegistry
 *  org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
 */
package cn.game.board.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
implements WebSocketMessageBrokerConfigurer {
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(new String[]{"/topic", "/user"});
        registry.setApplicationDestinationPrefixes(new String[]{"/app"});
        registry.setUserDestinationPrefix("/user");
    }

    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(new String[]{"/ws"}).setAllowedOriginPatterns(new String[]{"*"}).withSockJS();
    }
}

