package com.diploma.doc_classifier.service;

import com.diploma.doc_classifier.model.User;
import com.diploma.doc_classifier.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Отримуємо email користувача від Microsoft
        String email = oAuth2User.getAttribute("email");

        // Якщо email прийшов пустим (буває рідко), пробуємо взяти "preferred_username"
        if (email == null) {
            email = oAuth2User.getAttribute("preferred_username");
        }

        // Перевіряємо, чи є такий користувач в базі
        if (email != null) {
            String finalEmail = email;
            userRepository.findByUsername(finalEmail).orElseGet(() -> {
                // Якщо немає - створюємо нового
                User newUser = new User();
                newUser.setUsername(finalEmail);
                newUser.setPassword(""); // Пароль пустий, бо вхід через Microsoft
                newUser.setRole("USER");
                return userRepository.save(newUser);
            });
        }

        return oAuth2User;
    }
}