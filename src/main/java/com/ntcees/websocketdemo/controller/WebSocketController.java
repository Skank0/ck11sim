package com.ntcees.websocketdemo.controller;

import com.ntcees.websocketdemo.model.SignalData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@EnableWebSocketMessageBroker
public class WebSocketController implements ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Хранилище активных сигналов: id -> текущее значение
    private final Map<String, Double> activeSignals = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicInteger counter = new AtomicInteger(0);

    @PostConstruct
    public void startSignalGenerator() {
        // Генерируем случайные сигналы каждые 2 секунды
        scheduler.scheduleAtFixedRate(this::generateSignals, 0, 2, TimeUnit.SECONDS);
    }

    private void generateSignals() {
        activeSignals.keySet().forEach(id -> {
            double newValue = Math.sin(System.currentTimeMillis() / 1000.0 + counter.getAndIncrement()) * 100;
            activeSignals.put(id, newValue);
            SignalData data = new SignalData(id, newValue);
            messagingTemplate.convertAndSend("/topic/signals/" + id, data);
        });
    }

    public void addSignal(String id) {
        activeSignals.put(id, 0.0);
    }

    public void removeSignal(String id) {
        activeSignals.remove(id);
    }

    @Override
    public void onApplicationEvent(org.springframework.context.event.ContextRefreshedEvent event) {
        // Инициализация после старта контекста
    }
}