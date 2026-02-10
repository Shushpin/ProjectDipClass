package com.diploma.doc_classifier.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String filePath;
    private String category;
    private Double confidence;
    private LocalDateTime uploadDate;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String content;

    // --- 👇 ДОДАЙТЕ ЦЮ ЧАСТИНУ 👇 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User uploader;

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
    }
    // ---------------------------------

    // --- Інші Геттери та Сеттери ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}