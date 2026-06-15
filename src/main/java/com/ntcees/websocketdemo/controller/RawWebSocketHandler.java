package com.ntcees.websocketdemo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ntcees.websocketdemo.model.SignalData;
import com.ntcees.websocketdemo.model.SignalDataList;
import com.ntcees.websocketdemo.model.SignalValueList;
import com.ntcees.websocketdemo.utils.FileProcessingService;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final Map<String, Map<Integer, Double>> activeSignals = new ConcurrentHashMap<>();

    // Хранилище сигналов, для которых нужна плохая метка качества: id -> текущее значение
    private final Map<String, Map<Integer, Long>> signalsQualityManual = new ConcurrentHashMap<>();

    // Маппинг: sessionId -> каналы, на которые подписан клиент
    private static final Map<String, WebSocketSession> clientSubscriptions = new ConcurrentHashMap<>();

    private static List<String> uidPlans = new ArrayList<>();

    private final ScheduledExecutorService schedulerCurrentMeters1 = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService schedulerCurrentMeters2 = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService schedulerAuth = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService schedulerWebSocketCloser = Executors.newScheduledThreadPool(2);
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicBoolean isAuth = new AtomicBoolean(true);
    private final AtomicBoolean isRand = new AtomicBoolean(false);
    private final AtomicBoolean isKeepSignals = new AtomicBoolean(true);

    public boolean getIsAuth() {
        return isAuth.get();
    }

    public void setIsAuth(boolean newValue) {
        isAuth.set(newValue);
    }

    public void setIsRand(boolean newValue) {
        isRand.set(newValue);
    }

    @PostConstruct
    public void init() {
        // Запускаем генератор данных
        schedulerCurrentMeters1.scheduleAtFixedRate(this::generateAndBroadcastSignals, 0, 2, TimeUnit.SECONDS);
        schedulerCurrentMeters2.scheduleAtFixedRate(this::generateAndBroadcastSignalsPlans, 0, 10, TimeUnit.SECONDS);
        schedulerAuth.scheduleAtFixedRate(this::setIsAuthFalse, 12000, 12000, TimeUnit.SECONDS);
        schedulerWebSocketCloser.scheduleAtFixedRate(this::closeAllWebSocketConnections, 0, 150, TimeUnit.SECONDS);

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resourceUrl = classLoader.getResource("uidPlans.txt");
            uidPlans = FileProcessingService.readTrimmedNonEmptyLines(Paths.get(resourceUrl.toURI()));
        } catch (Exception e) {
            log.info("uidPlans.txt не загружен");
        }
    }



    private void setIsAuthFalse() {
        setIsAuth(false);
    }

    @PreDestroy
    public void destroy() {
        schedulerCurrentMeters1.shutdown();
        schedulerCurrentMeters2.shutdown();
        schedulerAuth.shutdown();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("Клиент подключился: {}, IP: {}", sessionId, session.getRemoteAddress());
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
            if (!isKeepSignals.get()) {
                activeSignals.clear();
            }
        }
    }

    public void setSignalValue48(String channel, int number, double value, boolean isManual) {
        for (int i = 0; i < 48; i++) {
            setSignalValue(channel, i, value, isManual);
        }
    }

    public void setSignalValue(String channel, int number, double value, boolean isManual) {
        if (number == -1) {
            setSignalValue48(channel, number, value, isManual);
        } else {
            if (activeSignals.containsKey(channel)) {
                activeSignals.get(channel).put(number, value);
            } else {
                activeSignals.put(channel, new HashMap<>(Map.of(number, value)));
            }
            if (isManual) {
                log.info("[MANUAL] Значение параметра {} [{}] задано равным {}", channel, number, value);
            }
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
            log.info("isAuth == false. Отмена подписки");
            return null;
        }

        SignalValueList valueList = new SignalValueList();
        SignalDataList dataList = new SignalDataList();
        activeSignals.keySet().forEach(channel -> {

            if ( (isPlans && uidPlans.contains(channel)) ||
                 (!isPlans && !uidPlans.contains(channel))) {

                int meters2DaysCount = 48;
                int count = 1;
                if (isPlans) count = meters2DaysCount;

                LocalDate date = LocalDate.now(); // Получаем сегодняшнюю дату
                LocalDateTime midnight = date.atStartOfDay();

                Map<Integer, Long> signalsQualityManualElement = null;
                if (signalsQualityManual.containsKey(channel)) {
                    signalsQualityManualElement = signalsQualityManual.get(channel);
                }

                for (int i = 0; i < count; i++) {
                    double newValue = 0;
                    if (isRand.get()) {
                        newValue = Math.sin(System.currentTimeMillis() / 1000.0 + counter.getAndIncrement()) * 100;
                    } else {
                        if (activeSignals.containsKey(channel)) {
                            if (activeSignals.get(channel).containsKey(i)) {
                                newValue = activeSignals.get(channel).get(i);
                            }
                        }
                    }
                    setSignalValue(channel, i, newValue, false);

                    SignalData data = new SignalData(channel, newValue);

                    if (isPlans) {
                        data.setTimeStamp(Instant.ofEpochSecond(midnight.toEpochSecond(ZoneOffset.UTC) + 3600 * i).toString());
                        data.setTimeStamp2(data.getTimeStamp());
                    } else {
                        //data.setTimeStamp(Instant.ofEpochSecond (midnight.toEpochSecond(ZoneOffset.UTC) + 3600 * (meters2DaysCount - 1)).toString());
                        data.setTimeStamp(data.getTimeStamp());
                        data.setTimeStamp2(data.getTimeStamp());
                    }

                    if (signalsQualityManualElement != null) {
                        if (signalsQualityManualElement.containsKey(i)) {
                            data.setqCode(signalsQualityManualElement.get(i));
                        }
                    }

                    if (sendToWebSocket) {
                        dataList.getData().getData().add(data);
                    } else {
                        valueList.getValue().add(data);
                    }
                }
            }
        });

        if (sendToWebSocket && !dataList.getData().getData().isEmpty()) {
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
        synchronized (clientSubscriptions) {
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
    }

    private void closeAllWebSocketConnections() {
        synchronized (clientSubscriptions) {
            for (Map.Entry<String, WebSocketSession> entry : clientSubscriptions.entrySet()) {
                WebSocketSession session = entry.getValue();
                try {
                    if (session != null && session.isOpen()) {
                        session.close(); // Закрыть соединение
                        log.info("Закрытие соединения с {}", session.getRemoteAddress());
                    }
                } catch (Exception e) {
                    // Логирование ошибки, если что-то пошло не так
                    log.error("Error closing WebSocket session: {}", e.getMessage());
                }
            }
            clientSubscriptions.clear();
        }
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
        if (!activeSignals.containsKey(id)) {
            activeSignals.put(id, new HashMap<>(Map.of(0, 0.0)));
        }
    }

    public void removeSignal(String id) {
        log.debug("removeSignal:{}", id);
        activeSignals.remove(id);
    }

    public void addSignalQuality(String id, Integer element, Long qualityValue) {
        log.debug("addSignalQuality:{} {} {}", id, element, qualityValue);
        if (!signalsQualityManual.containsKey(id)) {
            signalsQualityManual.put(id, new HashMap<>(Map.of(element, qualityValue)));
        } else {
            signalsQualityManual.get(id).put(element, qualityValue);
        }
    }


}