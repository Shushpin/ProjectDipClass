package com.diploma.doc_classifier.controller;

import com.diploma.doc_classifier.model.User;
import com.diploma.doc_classifier.repository.UserRepository;
import com.diploma.doc_classifier.service.DocumentService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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

    // Сторінка входу
    @GetMapping("/login")
    public String login() {
        return "login"; // Поверне файл login.html
    }

    // Сторінка реєстрації (форма)
    @GetMapping("/register")
    public String register() {
        return "register"; // Поверне файл register.html
    }

    // Обробка реєстрації (коли натиснули кнопку "Зареєструватися")
    @PostMapping("/register")
    public String processRegister(User user) {
        // Шифруємо пароль перед збереженням
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userRepository.save(user);
        return "redirect:/login"; // Перенаправляємо на вхід
    }

    // Головна сторінка (Dashboard)
    @GetMapping("/dashboard")
    public String dashboard(Model model, @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.core.user.OAuth2User principal) {
        // Отримуємо всі документи
        var docs = documentService.getAllDocuments();

        // Передаємо документи
        model.addAttribute("documents", docs);

        // --- Статистика для карток ---
        model.addAttribute("totalDocs", docs.size());

        // Рахуємо кількість документів з високою точністю (> 80%)
        long highConfDocs = docs.stream().filter(d -> d.getConfidence() != null && d.getConfidence() > 0.8).count();
        model.addAttribute("highConfidenceCount", highConfDocs);

        // Отримуємо ім'я користувача (якщо через Microsoft - беремо name, якщо ні - username)
        String username = "Користувач";
        if (principal != null) {
            username = principal.getAttribute("name"); // Ім'я з Microsoft (напр. Denys Velychko)
        }
        model.addAttribute("username", username);

        return "dashboard";
    }

    // Перенаправлення з кореня на логін
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}