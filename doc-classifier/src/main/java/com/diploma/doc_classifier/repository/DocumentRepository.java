package com.diploma.doc_classifier.repository;

import com.diploma.doc_classifier.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    // Тут поки пусто. JpaRepository вже дає нам методи save(), findById(), findAll().
    // Пізніше ми зможемо додати сюди пошук, наприклад:
    // List<Document> findByCategory(String category);
}