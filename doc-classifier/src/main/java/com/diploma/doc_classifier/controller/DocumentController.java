package com.diploma.doc_classifier.controller;

import com.diploma.doc_classifier.model.Document;
import com.diploma.doc_classifier.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Document savedDoc = documentService.saveDocument(file);
            return ResponseEntity.ok("Файл успішно завантажено! ID: " + savedDoc.getId());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Помилка завантаження файлу: " + e.getMessage());
        }
    }
}