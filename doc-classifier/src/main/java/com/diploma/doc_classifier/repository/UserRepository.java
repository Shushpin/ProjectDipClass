package com.diploma.doc_classifier.repository;

import com.diploma.doc_classifier.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Магічний метод Spring Data: шукає користувача за логіном
    Optional<User> findByUsername(String username);
}