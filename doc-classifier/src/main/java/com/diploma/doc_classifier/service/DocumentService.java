package com.diploma.doc_classifier.service;

import com.diploma.doc_classifier.model.ClassificationResponse; // <--- Наш новий клас
import com.diploma.doc_classifier.model.Document;
import com.diploma.doc_classifier.repository.DocumentRepository;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // <--- Для запитів до Python
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final Path fileStorageLocation;
    private final Tika tika;
    private final RestTemplate restTemplate; // <--- Інструмент для запитів

    // Адреса нашого Python сервісу
    private final String PYTHON_SERVICE_URL = "http://127.0.0.1:5000/classify";

    public DocumentService(DocumentRepository documentRepository,
                           @Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.documentRepository = documentRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
        this.tika = new Tika();
        this.restTemplate = new RestTemplate(); // <--- Ініціалізація
    }

    public Document saveDocument(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // 1. Витягуємо текст
        String extractedText = "";
        try {
            extractedText = tika.parseToString(targetLocation);
        } catch (Exception e) {
            extractedText = ""; // Якщо не вийшло, буде пустий текст
        }

        // 2. Створюємо об'єкт документа
        Document doc = new Document();
        doc.setFilename(fileName);
        doc.setFilePath(targetLocation.toString());
        doc.setUploadDate(LocalDateTime.now());
        doc.setContent(extractedText);

        // --- НОВА ЧАСТИНА: Виклик AI ---
        if (extractedText != null && !extractedText.isEmpty()) {
            try {
                // Готуємо дані для відправки (JSON: {"text": "..."})
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("text", extractedText);

                // Відправляємо POST запит на Python і чекаємо відповідь класу ClassificationResponse
                ClassificationResponse response = restTemplate.postForObject(
                        PYTHON_SERVICE_URL,
                        requestBody,
                        ClassificationResponse.class
                );

                // Якщо відповідь прийшла - записуємо в документ
                if (response != null) {
                    doc.setCategory(response.category());
                    doc.setConfidence(response.confidence());
                    doc.setStatus("CLASSIFIED");

                    System.out.println("AI Response: " + response.category() + " " + response.confidence());
                }

            } catch (Exception e) {
                // Якщо Python вимкнений або помилка
                System.err.println("AI Service Error: " + e.getMessage());
                doc.setStatus("UPLOADED_BUT_NOT_CLASSIFIED");
            }
        } else {
            doc.setStatus("NO_TEXT_EXTRACTED");
        }
        // -------------------------------

        return documentRepository.save(doc);
    }
}