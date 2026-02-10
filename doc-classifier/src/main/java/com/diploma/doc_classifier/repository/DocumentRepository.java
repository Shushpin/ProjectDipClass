package com.diploma.doc_classifier.repository;

import com.diploma.doc_classifier.model.Document;
import com.diploma.doc_classifier.model.User; // <--- Обов'язково цей імпорт!
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    // Ось цей метод, якого не вистачало Java:
    List<Document> findByUploader(User uploader);
}