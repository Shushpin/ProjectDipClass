package com.diploma.doc_classifier.model;

// Цей клас потрібен, щоб "зловити" відповідь від Python
public record ClassificationResponse(String category, Double confidence) {
}