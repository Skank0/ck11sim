package com.ntcees.websocketdemo.controller;

import com.ntcees.websocketdemo.model.SignalData;
import com.ntcees.websocketdemo.model.SignalDataList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static com.ntcees.websocketdemo.controller.RestControllerCk11.sendJsonAnswer;
import static java.lang.System.*;

@Controller
@EnableWebSocketMessageBroker
public class WebSocketController implements ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> {

    public static final String TOPIC = "/pubchan-OGjXXUCae-LKlRoL_ib8Vg";

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Хранилище активных сигналов: id -> текущее значение
    private final Map<String, Double> activeSignals = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicInteger counter = new AtomicInteger(0);

    private final Map<String, String> clientStates = new ConcurrentHashMap<>();

    @PostConstruct
    public void startSignalGenerator() {
        // Генерируем случайные сигналы каждые 2 секунды
        scheduler.scheduleAtFixedRate(this::generateSignals, 0, 2, TimeUnit.SECONDS);
    }

    private void generateSignals() {
        clientStates.keySet().forEach(id -> {
            if (Objects.equals(clientStates.get(id), "justConnected")) {
                ResponseEntity<String> firstMessage = sendJsonAnswer("../../13 linux/00_helps/server_answers/first_message_result.json");
                messagingTemplate.convertAndSend("", firstMessage);
                clientStates.put(id, "Connected");
                log.debug("session {} -> Connected", id);
            }
        });


        activeSignals.keySet().forEach(id -> {
            double newValue = Math.sin(currentTimeMillis() / 1000.0 + counter.getAndIncrement()) * 100;
            activeSignals.put(id, newValue);
            SignalData data = new SignalData(id, newValue);
            SignalDataList list = new SignalDataList();
            list.getValue().add(data);
            messagingTemplate.convertAndSend(WebSocketController.TOPIC, list);
        });

        //todo:
        //java.lang.IllegalStateException: Message will not be sent because the WebSocket session has been closed
    }

    public void addSignal(String id) {
        log.debug("addSignal:{}", id);
        activeSignals.put(id, 0.0);
    }

    public void removeSignal(String id) {
        log.debug("removeSignal:{}", id);
        activeSignals.remove(id);
    }

    @Override
    public void onApplicationEvent(org.springframework.context.event.ContextRefreshedEvent event) {
        // Инициализация после старта контекста
        log.info("onApplicationEvent");
    }


    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info("Клиент подключился: {}", event.getMessage().getHeaders().get("simpSessionId"));
        // Здесь можно инициализировать данные для нового клиента
        clientStates.put((String)event.getMessage().getHeaders().get("simpSessionId"), "justConnected");
        log.debug("session {} -> justConnected", (String) event.getMessage().getHeaders().get("simpSessionId"));
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("Клиент отключился: {}", event.getMessage().getHeaders().get("simpSessionId"));
        // Опционально: очистка ресурсов
        clientStates.put((String)event.getMessage().getHeaders().get("simpSessionId"), "justDisconnected");
        log.debug("session {} -> justDisconnected", (String) event.getMessage().getHeaders().get("simpSessionId"));
    }
}