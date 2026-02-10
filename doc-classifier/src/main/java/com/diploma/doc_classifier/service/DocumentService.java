package com.diploma.doc_classifier.service;

import com.diploma.doc_classifier.model.ClassificationResponse;
import com.diploma.doc_classifier.model.Document;
import com.diploma.doc_classifier.model.User;
import com.diploma.doc_classifier.repository.DocumentRepository;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final Path fileStorageLocation;
    private final Tika tika;
    private final RestTemplate restTemplate;

    private final String PYTHON_SERVICE_URL = "http://127.0.0.1:5000/classify";

    public DocumentService(DocumentRepository documentRepository,
                           @Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.documentRepository = documentRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
        this.tika = new Tika();
        this.restTemplate = new RestTemplate();
    }

    public Document saveDocument(MultipartFile file, User user) throws IOException {
        String fileName = file.getOriginalFilename();
        Path targetLocation = this.fileStorageLocation.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        String extractedText = "";
        try {
            extractedText = tika.parseToString(targetLocation);
        } catch (Exception e) {
            extractedText = "";
        }

        Document doc = new Document();
        doc.setFilename(fileName);
        doc.setFilePath(targetLocation.toString());
        doc.setUploadDate(LocalDateTime.now());
        doc.setContent(extractedText);
        doc.setUploader(user); // Прив'язуємо власника

        // Виклик AI
        if (extractedText != null && !extractedText.isEmpty()) {
            try {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("text", extractedText);

                ClassificationResponse response = restTemplate.postForObject(
                        PYTHON_SERVICE_URL, requestBody, ClassificationResponse.class);

                if (response != null) {
                    doc.setCategory(response.category());
                    doc.setConfidence(response.confidence());
                    doc.setStatus("CLASSIFIED");
                }
            } catch (Exception e) {
                System.err.println("AI Error: " + e.getMessage());
                doc.setStatus("ERROR");
            }
        } else {
            doc.setStatus("NO_TEXT");
        }

        return documentRepository.save(doc);
    }

    // Метод для АДМІНА (вертає все)
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // Метод для СТУДЕНТА (вертає тільки його файли)
    public List<Document> getDocumentsByUser(User user) {
        return documentRepository.findByUploader(user);
    }
    // ... існуючий код ...

    public void deleteDocument(Long id, User currentUser) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Документ не знайдено"));

        // ПЕРЕВІРКА ПРАВ: Видаляє або Адмін, або Власник
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isOwner = doc.getUploader() != null && doc.getUploader().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("У вас немає прав на видалення цього файлу!");
        }

        // 1. Видаляємо фізичний файл з диска
        try {
            if (doc.getFilePath() != null) {
                Path path = Paths.get(doc.getFilePath());
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            System.err.println("Не вдалося видалити файл з диска: " + e.getMessage());
            // Продовжуємо, щоб хоча б з бази видалити
        }

        // 2. Видаляємо запис з бази даних
        documentRepository.delete(doc);
    }
}