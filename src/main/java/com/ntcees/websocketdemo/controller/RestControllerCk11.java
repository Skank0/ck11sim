package com.ntcees.websocketdemo.controller;


import com.ntcees.websocketdemo.model.SignalData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
public class RestControllerCk11 {

    @Autowired
    private WebSocketController wsController;

    // Создание нового сигнала
    @PostMapping("/api/signal")
    public ResponseEntity<String> createSignal(@RequestBody SignalData request) {
        if (request.getId() == null || request.getId().isBlank()) {
            return ResponseEntity.badRequest().body("ID не может быть пустым");
        }
        wsController.addSignal(request.getId());
        return ResponseEntity.ok("Сигнал '" + request.getId() + "' создан. Подключайтесь к " + WebSocketController.TOPIC);
    }

    // Удаление сигнала
    @DeleteMapping("/api/signal/{id}")
    public ResponseEntity<String> deleteSignal(@PathVariable String id) {
        wsController.removeSignal(id);
        return ResponseEntity.ok("Сигнал '" + id + "' удалён");
    }

    // Получение текущего значения сигнала
    @GetMapping("/signal/{id}")
    public ResponseEntity<SignalData> getSignal(@PathVariable String id) {
        // В реальном приложении здесь можно было бы хранить последние значения
        return ResponseEntity.ok(new SignalData(id, Math.random() * 100));
    }


    @GetMapping("/api/public/core/v2.1/addresses")
    public ResponseEntity<String> getAddresses() {
        return sendJsonAnswer("../../13 linux/00_helps/server_answers/addresses.json");
    }

    @GetMapping("/auth/app/token")
    public ResponseEntity<String> getToken() {
        return sendJsonAnswer("../../13 linux/00_helps/server_answers/token.json");
    }


    //TrymakeRequestToUpdateValues_TEK
    @GetMapping("/api/public/measurement-values/v2.1/numeric/data/get-snapshot")
    //todo: сделать генерацию измерений
    public ResponseEntity<String> getSnapshot() {
        return sendJsonAnswer("../../13 linux/00_helps/server_answers/multi_meters_now_request.json");
    }

    //TrymakeRequestToUpdateValues_TAB
    @GetMapping("/api/public/measurement-values/v2.1/numeric/data/get-table")
    //todo: сделать генерацию измерений
    public ResponseEntity<String> getTable() {
        return sendJsonAnswer("../../13 linux/00_helps/server_answers/multi_meter_interval_request.json");
    }

    @PostMapping("/api/public/measurement-values/v2.1/data/subscriptions/channels/pubchan-OGjXXUCae-LKlRoL_ib8Vg/subscriptions/mv-34")
    //todo: сделать генерацию измерений
    //todo: сделать добавление uid в список для рассылки
    public ResponseEntity<String> createSubscription(@RequestBody Map<String, Object> payload) {
        if (payload.containsKey("subscriptionType")) {
            return sendJsonAnswer("../../13 linux/00_helps/server_answers/create_subscription_result.json");
        } else if (payload.containsKey("measurementValueToAddUids")) {
            List<String> list = (List<String>)payload.get("measurementValueToAddUids");
            for (String element : list) {
                wsController.addSignal(element);
            }
            return sendJsonAnswer("../../13 linux/00_helps/server_answers/meter_result.json");
        } else {
            return ResponseEntity.badRequest().body("Неизвестный формат запроса");
        }
    }



    //отправка ответом json-файла
    static ResponseEntity<String> sendJsonAnswer(String pathname) {
        try {
            File file = new File(pathname);
            String jsonContent = Files.readString(Path.of(file.getAbsolutePath()));
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(jsonContent);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}