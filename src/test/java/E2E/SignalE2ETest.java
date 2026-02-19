package E2E;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntcees.ApplicationCK11sim;
import com.ntcees.websocketdemo.controller.WebSocketController;
import com.ntcees.websocketdemo.model.SignalData;
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
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ApplicationCK11sim.class)
public class SignalE2ETest {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    @BeforeEach
    void setup() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    void shouldReceiveSignalAfterSubscription() throws Exception {
        // 1. Отправляем POST-запрос для регистрации сигнала
        String signalId;
        String jsonBody;
        String topic = WebSocketController.TOPIC;
        HttpEntity<String> request;
        SignalData message;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Подключаемся к WebSocket
        String wsUrl = "ws://localhost:" + port + "/api/public/core/v2.1/channels/open";
        ListenableFuture<StompSession> sessionFuture = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {});
        stompSession = sessionFuture.get(10, TimeUnit.SECONDS);

        jsonBody = objectMapper.writeValueAsString(Map.of("subscriptionType", "written"));
        request = new HttpEntity<>(jsonBody, headers);
        restTemplate.postForEntity("http://localhost:" + port + "/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions/mv-34", request, Void.class);


        List<String> signalListGuid = List.of("38ca00a5-fbdb-44a1-9f32-10d065804165", "610577b7-c37e-42e0-a753-b4e5a87afbb9");
        jsonBody = objectMapper.writeValueAsString(Map.of("measurementValueToAddUids", signalListGuid));
        request = new HttpEntity<>(jsonBody, headers);
        restTemplate.postForEntity("http://localhost:" + port + "/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions/mv-34", request, Void.class);

        BlockingQueue<Object> receivedMessagesGuided = new LinkedBlockingQueue<>();
        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class; // ← принимаем "сырые" байты
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof byte[] bytes) {
                    String rawJson = new String(bytes, StandardCharsets.UTF_8);
                    log.info("Сырое сообщение от сервера: {}", rawJson);
                    // Попробуем вручную десериализовать
                    try {
                        SignalData data = objectMapper.readValue(rawJson, SignalData.class);
                        receivedMessagesGuided.offer(data);
                    } catch (Exception e) {
                        log.error("Ошибка десериализации: {}", e.getMessage());
                    }
                }
            }
        });

        for (int i = 0; i < 10; i++) {
            message = (SignalData)receivedMessagesGuided.poll(10, TimeUnit.SECONDS);
            assertThat(message).withFailMessage("Не получено сообщения SignalData за 10 секунд").isNotNull();
            assertThat(signalListGuid.contains(message.getUid()));
            assertThat(message.getValue()).isNotNull();
            log.info("message=" + message);
        }
    }
}