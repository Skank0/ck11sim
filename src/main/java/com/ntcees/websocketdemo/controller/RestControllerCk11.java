package com.ntcees.websocketdemo.controller;


import com.ntcees.websocketdemo.model.SignalData;
import com.ntcees.websocketdemo.model.SignalDataList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
public class RestControllerCk11 {

    @Autowired
    private RawWebSocketHandler  rawWebSocketHandler;

    // Создание нового сигнала
    @PostMapping("/api/signal")
    public ResponseEntity<String> createSignal(@RequestBody SignalData request) {
        if (request.getUid() == null || request.getUid().isBlank()) {
            return ResponseEntity.badRequest().body("ID не может быть пустым");
        }
        rawWebSocketHandler.addSignal(request.getUid());
        return ResponseEntity.ok("Сигнал '" + request.getUid() + "' создан. Подключайтесь к " + RawWebSocketHandler.TOPIC);
    }

    // Удаление сигнала
    @DeleteMapping("/api/signal/{id}")
    public ResponseEntity<String> deleteSignal(@PathVariable String id) {
        rawWebSocketHandler.removeSignal(id);
        return ResponseEntity.ok("Сигнал '" + id + "' удалён");
    }

    // Получение текущего значения сигнала
    @GetMapping("/signal/{id}")
    public ResponseEntity<SignalData> getSignal(@PathVariable String id) {
        // В реальном приложении здесь можно было бы хранить последние значения
        return ResponseEntity.ok(new SignalData(id, Math.random() * 100));
    }


    //todo: port to autowired
    @GetMapping("/api/public/core/v2.1/addresses")
    public ResponseEntity<String> getAddresses() {
        return sendJsonAnswer("../../13 linux/00_helps/server_answers/addresses.json",
                List.of("https://app-p-srv-balt.oducz.so:9443", "https://app-p-srv-balt.oducz.so"),
                List.of("http://10.31.224.135:8080", "http://10.31.224.135:8080"));
    }

    @PostMapping("/auth/app/token")
    public ResponseEntity<String> getToken(@RequestBody Map<String, Object> payload) {
        return sendJsonAnswer("../../13 linux/00_helps/server_answers/token.json");
    }


    //TrymakeRequestToUpdateValues_TEK
    @PostMapping("/api/public/measurement-values/v2.1/numeric/data/get-snapshot")
    //todo: сделать генерацию измерений
    public ResponseEntity<String> getSnapshot() {
        //return sendJsonAnswer("../../13 linux/00_helps/server_answers/multi_meters_now_request.json");
        SignalDataList signalDataList = rawWebSocketHandler.generateAndBroadcastSignals(false);
        if (signalDataList != null) {
            String json = rawWebSocketHandler.toJson(signalDataList);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(json);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    //TrymakeRequestToUpdateValues_TAB
    @PostMapping("/api/public/measurement-values/v2.1/numeric/data/get-table")
    //todo: сделать генерацию измерений
    public ResponseEntity<String> getTable() {
        return sendJsonAnswer("../../13 linux/00_helps/server_answers/multi_meter_interval_request.json");
    }

    @PatchMapping("/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions/mv-34")
    //todo: сделать генерацию измерений
    //todo: сделать добавление uid в список для рассылки
    public ResponseEntity<String> addToSubscription(@RequestBody Map<String, Object> payload) {
        if (payload.containsKey("measurementValueToAddUids")) {
            List<String> list = (List<String>)payload.get("measurementValueToAddUids");
            for (String element : list) {
                rawWebSocketHandler.addSignal(element);
            }
            return sendJsonAnswer("../../13 linux/00_helps/server_answers/meter_result.json");
        } else {
            return ResponseEntity.badRequest().body("Неизвестный формат запроса");
        }
    }

    @PostMapping("/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions")
    public ResponseEntity<String> createSubscription(@RequestBody Map<String, Object> payload) {
        if (payload.containsKey("subscriptionType")) {
            ResponseEntity<String> answer = sendJsonAnswer("../../13 linux/00_helps/server_answers/create_subscription_result.json");
            return ResponseEntity.created(URI.create("/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions/mv-34"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(answer.getBody());
        } else {
            return ResponseEntity.badRequest().body("Неизвестный формат запроса");
        }
    }


    //отправка ответом json-файла
    static ResponseEntity<String> sendJsonAnswer(String pathname, List<String> whatReplace, List<String> newValue) {
        try {
            File file = new File(pathname);
            String jsonContent = Files.readString(Path.of(file.getAbsolutePath()));
            if (whatReplace != null && !whatReplace.isEmpty() && whatReplace.size() == newValue.size()) {
                for (int i = 0; i < whatReplace.size() && i < newValue.size(); i++) {
                    jsonContent = jsonContent.replace(whatReplace.get(i), newValue.get(i));
                }
            }
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(jsonContent);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    static ResponseEntity<String> sendJsonAnswer(String pathname) {
        return sendJsonAnswer(pathname, null, null);
    }
}