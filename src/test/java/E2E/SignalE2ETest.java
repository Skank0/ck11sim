package E2E;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntcees.ApplicationCK11sim;
import com.ntcees.websocketdemo.controller.RawWebSocketHandler;
import com.ntcees.websocketdemo.model.SignalDataList;
import com.ntcees.websocketdemo.model.SignalValueList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ApplicationCK11sim.class)
class SignalE2ETest {

    private static final Logger log = LoggerFactory.getLogger(RawWebSocketHandler.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketHttpHeaders headers;
    private StandardWebSocketClient client;
    private WebSocketConnectionManager connectionManager;
    private LinkedBlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();

    @BeforeEach
    void setup() {
        headers = new WebSocketHttpHeaders();
        client = new StandardWebSocketClient();
        // Подключаемся к эндпоинту
        URI uri = URI.create("ws://localhost:" + port + "/api/public/core/v2.1/channels/open");
        connectionManager = new WebSocketConnectionManager(client, new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {

                Map<String, Object> subscribeMsg = Map.of(
                        "type", "subscribe",
                        "channel", "pubchan-OGjXXUCae-LKlRoL_ib8Vg"
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(subscribeMsg)));

                String signalId;
                String jsonBody;
                HttpEntity<String> request;

                jsonBody = objectMapper.writeValueAsString(Map.of("subscriptionType", "written"));
                request = new HttpEntity<>(jsonBody, headers);
                restTemplate.postForEntity("http://localhost:" + port + "/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions/mv-34", request, Void.class);

                List<String> signalListGuid = List.of("38ca00a5-fbdb-44a1-9f32-10d065804165", "610577b7-c37e-42e0-a753-b4e5a87afbb9");
                jsonBody = objectMapper.writeValueAsString(Map.of("measurementValueToAddUids", signalListGuid));
                request = new HttpEntity<>(jsonBody, headers);
                restTemplate.postForEntity("http://localhost:" + port + "/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions/mv-34", request, Void.class);

            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                receivedMessages.offer(message.getPayload());
            }
        }, String.valueOf(uri), headers);
        connectionManager.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null) {
            connectionManager.stop();
        }
    }

    @Test
    void shouldReceiveSignalData() throws Exception {
        // 1. Отправляем REST-запросы для регистрации
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        String jsonBody1 = objectMapper.writeValueAsString(Map.of("subscriptionType", "written"));
        HttpEntity<String> request1 = new HttpEntity<>(jsonBody1, httpHeaders);
        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions",
                request1, Void.class
        );

        List<String> signalListGuid = List.of(
                "38ca00a5-fbdb-44a1-9f32-10d065804165",
                "610577b7-c37e-42e0-a753-b4e5a87afbb9"
        );
        String jsonBody2 = objectMapper.writeValueAsString(Map.of("measurementValueToAddUids", signalListGuid));
        HttpEntity<String> request2 = new HttpEntity<>(jsonBody2, httpHeaders);
        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions/mv-34/post",
                request2, Void.class
        );

        // 2. Ждём сообщения
        // первое сообщение - не SignalDataList
        String rawJson = receivedMessages.poll(10, TimeUnit.SECONDS);
        assertThat(rawJson).withFailMessage("Не получено сообщение за 10 сек").isNotNull();

        // второе сообщение - SignalDataList
        rawJson = receivedMessages.poll(10, TimeUnit.SECONDS);
        assertThat(rawJson).withFailMessage("Не получено сообщение за 10 сек").isNotNull();

        // 3. Парсим как SignalDataList
        log.info("raw: {}",  rawJson);
        SignalDataList list = objectMapper.readValue(rawJson, SignalDataList.class);
        assertThat(list.getData().getData()).isNotEmpty();
        assertThat(list.getData().getData().get(0).getUid()).isIn(signalListGuid);
    }
}