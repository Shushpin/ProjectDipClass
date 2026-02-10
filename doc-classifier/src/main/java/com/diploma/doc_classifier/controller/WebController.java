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

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.core.user.OAuth2User principal,
                            java.security.Principal basicPrincipal) {

        String email;
        if (principal != null) {
            email = principal.getAttribute("email");
            if (email == null) email = principal.getAttribute("preferred_username");
        } else {
            email = basicPrincipal.getName();
        }

        String finalEmail = email; // Фіксуємо змінну для лямбди

        // --- ВИПРАВЛЕННЯ: Самовідновлення користувача ---
        // Якщо користувача немає в базі (наприклад, після перезапуску Docker), створюємо його.
        User currentUser = userRepository.findByUsername(finalEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(finalEmail);
                    newUser.setPassword(""); // Пароль пустий для OAuth
                    newUser.setRole("USER");
                    return userRepository.save(newUser);
                });

        // --- ЛОГІКА АДМІНА ---
        java.util.List<com.diploma.doc_classifier.model.Document> docs;
        boolean isAdmin = false;

        if ("ADMIN".equals(currentUser.getRole())) {
            // Адмін бачить ВСЕ
            docs = documentService.getAllDocuments();
            isAdmin = true;
        } else {
            // Студент бачить СВОЄ
            docs = documentService.getDocumentsByUser(currentUser);
        }
        // ---------------------

        model.addAttribute("documents", docs);
        model.addAttribute("totalDocs", docs.size());
        model.addAttribute("isAdmin", isAdmin); // Передаємо у HTML для відображення колонок

        // Статистику рахуємо по тому списку, який отримали
        long highConfDocs = docs.stream().filter(d -> d.getConfidence() != null && d.getConfidence() > 0.8).count();
        model.addAttribute("highConfidenceCount", highConfDocs);

        // Відображення імені (якщо є ім'я з Microsoft - беремо його, інакше - email)
        String username = (principal != null && principal.getAttribute("name") != null)
                ? principal.getAttribute("name")
                : currentUser.getUsername();
        model.addAttribute("username", username);

        return "dashboard";
    }}