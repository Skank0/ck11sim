package com.ntcees.websocketdemo.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/api/public/core/v2.1/channels/open")
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return new RawWebSocketHandler();
    }
}
