package com.ntcees.websocketdemo.controller;


import com.ntcees.websocketdemo.model.SignalData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestControllerCk11 {

    @Autowired
    private WebSocketController wsController;

    // Создание нового сигнала
    @PostMapping("/signal")
    public ResponseEntity<String> createSignal(@RequestBody SignalData request) {
        if (request.getId() == null || request.getId().isBlank()) {
            return ResponseEntity.badRequest().body("ID не может быть пустым");
        }
        wsController.addSignal(request.getId());
        return ResponseEntity.ok("Сигнал '" + request.getId() + "' создан. Подключайтесь к /topic/signals/" + request.getId());
    }

    // Удаление сигнала
    @DeleteMapping("/signal/{id}")
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
}