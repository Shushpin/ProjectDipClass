package com.diploma.doc_classifier.controller;

import com.diploma.doc_classifier.model.User;
import com.diploma.doc_classifier.repository.UserRepository;
import com.diploma.doc_classifier.service.DocumentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class WebController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentService documentService;

    public WebController(UserRepository userRepository, PasswordEncoder passwordEncoder, DocumentService documentService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.documentService = documentService;
    }

    // --- БЛОК 1: ВХІД ТА РЕЄСТРАЦІЯ ---

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    // --- БЛОК 2: ГОЛОВНА ПАНЕЛЬ (DASHBOARD) ---

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @AuthenticationPrincipal OAuth2User principal,
                            Principal basicPrincipal) {

        // 1. Визначаємо email (OAuth2 або звичайний логін)
        String email;
        if (principal != null) {
            email = principal.getAttribute("email");
            if (email == null) email = principal.getAttribute("preferred_username");
        } else {
            email = basicPrincipal.getName();
        }
        String finalEmail = email;

        // 2. Знаходимо або створюємо користувача (Self-Healing)
        User currentUser = userRepository.findByUsername(finalEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(finalEmail);
                    newUser.setPassword("");
                    newUser.setRole("USER");
                    return userRepository.save(newUser);
                });

        // 3. Визначаємо список документів залежно від ролі
        List<com.diploma.doc_classifier.model.Document> docs;
        boolean isAdmin = false;

        if ("ADMIN".equals(currentUser.getRole())) {
            docs = documentService.getAllDocuments(); // Адмін бачить все
            isAdmin = true;
        } else {
            docs = documentService.getDocumentsByUser(currentUser); // Юзер бачить своє
        }

        // 4. Передаємо основні дані в шаблон
        model.addAttribute("documents", docs);
        model.addAttribute("totalDocs", docs.size());
        model.addAttribute("isAdmin", isAdmin);

        // 5. Рахуємо статистику впевненості (> 80%)
        long highConfDocs = docs.stream()
                .filter(d -> d.getConfidence() != null && d.getConfidence() > 0.8)
                .count();
        model.addAttribute("highConfidenceCount", highConfDocs);

        // 6. Ім'я користувача для відображення
        String username = (principal != null && principal.getAttribute("name") != null)
                ? principal.getAttribute("name")
                : currentUser.getUsername();
        model.addAttribute("username", username);

        // --- БЛОК 3: ДАНІ ДЛЯ ДІАГРАМИ (Chart.js) ---

        // Групуємо документи по категоріях і рахуємо кількість
        Map<String, Long> stats = docs.stream()
                .collect(Collectors.groupingBy(
                        doc -> doc.getCategory() != null ? doc.getCategory() : "Невизначено",
                        Collectors.counting()
                ));

        // Розділяємо на списки для графіку
        List<String> labels = new ArrayList<>(stats.keySet());
        List<Long> data = new ArrayList<>(stats.values());

        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartData", data);

        return "dashboard";
    }

    // --- БЛОК 4: ВИДАЛЕННЯ ДОКУМЕНТІВ ---

    @PostMapping("/document/delete/{id}")
    public String deleteDocument(@PathVariable Long id,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 Principal basicPrincipal) {

        // Визначаємо хто видаляє
        String email;
        if (principal != null) {
            email = principal.getAttribute("email");
            if (email == null) email = principal.getAttribute("preferred_username");
        } else {
            email = basicPrincipal.getName();
        }

        User currentUser = userRepository.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Викликаємо сервіс (він сам перевірить права)
        documentService.deleteDocument(id, currentUser);

        return "redirect:/dashboard";
    }
    @GetMapping("/documents")
    public String allDocuments(Model model,
                               @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.core.user.OAuth2User principal,
                               java.security.Principal basicPrincipal) {

        // Отримуємо поточного користувача (boilerplate code, можна винести в окремий метод, але хай буде тут)
        String email;
        if (principal != null) {
            email = principal.getAttribute("email");
            if (email == null) email = principal.getAttribute("preferred_username");
        } else {
            email = basicPrincipal.getName();
        }

        User currentUser = userRepository.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Логіка відображення
        if ("ADMIN".equals(currentUser.getRole())) {
            // Якщо АДМІН -> показуємо список користувачів
            List<User> allUsers = userRepository.findAll();
            model.addAttribute("usersList", allUsers);
            model.addAttribute("isAdmin", true);
        } else {
            // Якщо ЮЗЕР -> показуємо його документи
            List<com.diploma.doc_classifier.model.Document> docs = documentService.getDocumentsByUser(currentUser);
            model.addAttribute("documents", docs);
            model.addAttribute("isAdmin", false);
        }

        model.addAttribute("username", currentUser.getUsername());
        return "documents"; // Повертаємо шаблон documents.html
    }

    // 2. Сторінка документів конкретного користувача (Тільки для Адміна)
    @GetMapping("/documents/{userId}")
    public String userDocuments(@PathVariable Long userId,
                                Model model,
                                @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.core.user.OAuth2User principal,
                                java.security.Principal basicPrincipal) {

        // Перевірка, чи це Адмін робить запит
        String email = (principal != null) ? principal.getAttribute("email") : basicPrincipal.getName();
        if (email == null && principal != null) email = principal.getAttribute("preferred_username");

        User adminUser = userRepository.findByUsername(email).orElseThrow();
        if (!"ADMIN".equals(adminUser.getRole())) {
            return "redirect:/dashboard"; // Звичайним юзерам сюди не можна
        }

        // Знаходимо цільового юзера
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Отримуємо документи ЦЬОГО юзера
        List<com.diploma.doc_classifier.model.Document> docs = documentService.getDocumentsByUser(targetUser);

        model.addAttribute("documents", docs);
        model.addAttribute("isAdmin", true);
        model.addAttribute("viewingUser", targetUser.getUsername()); // Щоб показати заголовок "Файли користувача X"
        model.addAttribute("username", adminUser.getUsername());

        return "documents";
    }
}