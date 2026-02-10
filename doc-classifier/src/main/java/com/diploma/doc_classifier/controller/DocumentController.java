package com.diploma.doc_classifier.controller;

import com.diploma.doc_classifier.model.Document;
import com.diploma.doc_classifier.model.User;
import com.diploma.doc_classifier.repository.UserRepository;
import com.diploma.doc_classifier.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository; // 1. Додаємо репозиторій

    public DocumentController(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             Authentication authentication) { // 2. Отримуємо дані про вхід
        try {
            // 3. Визначаємо email користувача (так само, як в WebController)
            String email;
            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                email = oauth2User.getAttribute("email");
                if (email == null) email = oauth2User.getAttribute("preferred_username");
            } else {
                email = authentication.getName();
            }

            // 4. Знаходимо користувача в базі
            String finalEmail = email;
            User user = userRepository.findByUsername(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + finalEmail));

            // 5. Передаємо користувача в метод збереження
            Document savedDoc = documentService.saveDocument(file, user);

            return ResponseEntity.ok("Файл успішно завантажено! ID: " + savedDoc.getId());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Помилка файлу: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Помилка доступу: " + e.getMessage());
        }
    }
}