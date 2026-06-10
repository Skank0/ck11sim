package com.ntcees.websocketdemo.utils;

import com.ntcees.websocketdemo.controller.RawWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CsvUploadController {

    @Autowired
    private RawWebSocketHandler rawWebSocketHandler;

    @PostMapping(value = "/load-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, List<Double>>> uploadCsv(
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", List.of()));
            }

            // Парсим CSV в Map<Заголовок, Список значений>
            Map<String, List<Double>> result = parseCsvToMap(file.getInputStream());

            for(Map.Entry<String, List<Double>>  entry : result.entrySet()) {
                String key = entry.getKey();
                int i = 0;
                for (Double d : entry.getValue()) {
                    rawWebSocketHandler.setSignalValue(key, i, d, true);
                    i++;
                }
            }

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", List.of(Double.NaN)));
        }
    }

    /**
     * Парсит CSV с разделителем ";" в Map<ColumnHeader, List<Double>>
     */
    private Map<String, List<Double>> parseCsvToMap(InputStream inputStream) throws IOException {
        Map<String, List<Double>> result = new LinkedHashMap<>(); // Сохраняем порядок колонок
        List<String> headers = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Разделяем по точке с запятой
                String[] values = line.split(";", -1); // -1 сохраняет пустые значения в конце

                if (lineNumber == 0) {
                    // Первая строка — заголовки
                    headers = Arrays.asList(values);
                    for (String header : headers) {
                        result.put(header.trim(), new ArrayList<>());
                    }
                } else {
                    // Строки с данными
                    if (headers != null) {
                        for (int i = 0; i < headers.size() && i < values.length; i++) {
                            String val = values[i].trim();
                            try {
                                // Парсим число: поддерживаем точку и запятую как десятичный разделитель
                                Double doubleVal = val.replace(',', '.').isEmpty() ? null :
                                        Double.parseDouble(val.replace(',', '.'));
                                if (doubleVal != null) {
                                    result.get(headers.get(i).trim()).add(doubleVal);
                                }
                            } catch (NumberFormatException e) {
                                // Пропускаем нечисловые значения (или можно добавить в лог)
                                // result.get(headers.get(i).trim()).add(null);
                            }
                        }
                    }
                }
                lineNumber++;
            }
        }

        return result;
    }
}