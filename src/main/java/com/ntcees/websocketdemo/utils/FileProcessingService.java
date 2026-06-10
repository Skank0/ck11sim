package com.ntcees.websocketdemo.utils;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileProcessingService {

    /**
     * Читает файл построчно, применяет trim() и возвращает только непустые строки.
     *
     @param filePath путь к файлу
      * @return List<String> список очищенных непустых строк
     * @throws IOException при ошибках чтения или если файл не существует
     */
    public static List<String> readTrimmedNonEmptyLines(Path filePath) throws IOException {
        if (filePath == null || !Files.exists(filePath)) {
           return new ArrayList<>();
        }

        // try-with-resources гарантирует закрытие Stream и дескриптора файла
        try (Stream<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)) {
            return lines
                    .map(String::trim)                     // 1. Убираем пробелы по краям
                    .filter(line -> !line.isEmpty())       // 2. Отсеиваем пустые строки
                    .collect(Collectors.toList());         // 3. Собираем в List
        }
    }
}
