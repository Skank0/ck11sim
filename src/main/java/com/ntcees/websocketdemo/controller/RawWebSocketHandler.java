package com.ntcees.websocketdemo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ntcees.websocketdemo.model.SignalData;
import com.ntcees.websocketdemo.model.SignalDataList;
import com.ntcees.websocketdemo.model.SignalValueList;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.handler.TextWebSocketHandler;


@Component
public class RawWebSocketHandler extends TextWebSocketHandler {

    public static final String TOPIC = "/pubchan-OGjXXUCae-LKlRoL_ib8Vg";

    private static final Logger log = LoggerFactory.getLogger(RawWebSocketHandler.class);

    // Хранилище активных сигналов: id -> текущее значение
    private final Map<String, Double> activeSignals = new ConcurrentHashMap<>();

    // Маппинг: sessionId -> каналы, на которые подписан клиент
    private static final Map<String, WebSocketSession> clientSubscriptions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService schedulerCurrentMeters = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService schedulerAuth = Executors.newScheduledThreadPool(2);
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicBoolean isAuth = new AtomicBoolean(false);

    public boolean getIsAuth() {
        return isAuth.get();
    }

    public void setIsAuth(boolean newValue) {
        isAuth.set(newValue);
    }

    @PostConstruct
    public void init() {
        // Запускаем генератор данных
        schedulerCurrentMeters.scheduleAtFixedRate(this::generateAndBroadcastSignals, 0, 1, TimeUnit.SECONDS);
        schedulerAuth.scheduleAtFixedRate(this::setIsAuthFalse, 0, 120, TimeUnit.SECONDS);
    }

    private void setIsAuthFalse() {
        setIsAuth(false);
    }

    @PreDestroy
    public void destroy() {
        schedulerCurrentMeters.shutdown();
        schedulerAuth.shutdown();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("Клиент подключился: {}", sessionId);
        clientSubscriptions.put(sessionId, session);

        // Отправляем первое сообщение
        sendJsonFile(session, "../../13 linux/00_helps/server_answers/first_message_result.json");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();

        try {
            // Ожидаем JSON вида: {"type":"subscribe","channel":"..."}
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(payload);

            if ("subscribe".equals(json.get("type").asText())) {
                String channel = json.get("channel").asText();
                log.debug("Клиент {} подписался на канал: {}", sessionId, channel);
            }

        } catch (Exception e) {
            log.error("Ошибка обработки сообщения от клиента {}: {}", sessionId, payload, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        log.info("Клиент отключился: {} (статус: {})", sessionId, status);
        clientSubscriptions.remove(sessionId);
        if (clientSubscriptions.isEmpty()) {
            activeSignals.clear();
        }
    }

    // Генерация и рассылка данных
    public SignalValueList generateAndBroadcastSignals(boolean sendToWebSocket, boolean isPlans) {
        if (activeSignals.isEmpty()) return null;

        if (!this.getIsAuth()) {
            SignalDataList list = new SignalDataList();
            list.setType("ru.monitel.ck11.exclusive-sub.aborted.v2");
            broadcast(list);
            closeAllSessions();
            return null;
        }

        SignalValueList valueList = new SignalValueList();
        SignalDataList dataList = new SignalDataList();
        activeSignals.keySet().forEach(channel -> {
            int count = 1;
            if (isPlans) count = 48;

            LocalDate date = LocalDate.now(); // Получаем сегодняшнюю дату
            LocalDateTime midnight = date.atStartOfDay();

            for (int i = 0; i < count; i++) {
                double newValue = Math.sin(System.currentTimeMillis() / 1000.0 + counter.getAndIncrement()) * 100;
                activeSignals.put(channel, newValue);

                SignalData data = new SignalData(channel, newValue);

                if (isPlans) {
                    data.setTimeStamp(Instant.ofEpochSecond (midnight.toEpochSecond(ZoneOffset.UTC) + 3600 * i).toString());
                    data.setTimeStamp2(data.getTimeStamp());
                }

                if (sendToWebSocket) {
                    dataList.getData().getData().add(data);
                } else {
                    valueList.getValue().add(data);
                }
            }
        });

        if (sendToWebSocket) {
            broadcast(dataList);
        }

        return valueList;
    }

    private void generateAndBroadcastSignals() {
        generateAndBroadcastSignals(true, false);
    }

    private void generateAndBroadcastSignalsPlans(){
        generateAndBroadcastSignals(true, true);
    }

    private void closeAllSessions() {
        clientSubscriptions.forEach((sessionId, webSocketSession) -> {
            try {
                webSocketSession.close();
            } catch (IOException e) {
                log.error("Не получается закрыть сессию");
            }
        });

        clientSubscriptions.clear();
    }

    // Рассылка по каналу
    private void broadcast(Object payload) {
        String json = toJson(payload);
        clientSubscriptions.forEach((sessionId, webSocketSession) -> {
            {
                if (webSocketSession != null && webSocketSession.isOpen()) {
                    try {
                        webSocketSession.sendMessage(new TextMessage(json));
                        log.debug("Отправка для сессии {} значение {}", webSocketSession.getId(), json);
                    } catch (Exception e) {
                        log.warn("Ошибка отправки в сессию {}: {}", sessionId, e.getMessage());
                    }
                }
            }
        });
    }


    private void sendJsonFile(WebSocketSession session, String resourcePath) {
        try {
            File file = new File(resourcePath);
            String content = Files.readString(Path.of(file.getAbsolutePath()));
            session.sendMessage(new TextMessage(content));
        } catch (Exception e) {
            log.error("Ошибка отправки файла клиенту", e);
        }
    }

    // Вспомогательные методы
    public String toJson(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации", e);
        }
    }




    public void addSignal(String id) {
        log.debug("addSignal:{}", id);
        activeSignals.put(id, 0.0);
    }

    public void removeSignal(String id) {
        log.debug("removeSignal:{}", id);
        activeSignals.remove(id);
    }

}