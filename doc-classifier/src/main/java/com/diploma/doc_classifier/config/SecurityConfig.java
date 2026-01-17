package com.diploma.doc_classifier.config;

import com.diploma.doc_classifier.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        // Сторінки реєстрації та входу доступні всім
                        .requestMatchers("/", "/register", "/login", "/css/**", "/js/**").permitAll()
                        // Будь-які інші запити вимагають авторизації
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .loginPage("/login") // Вказуємо нашу власну сторінку входу
                        .defaultSuccessUrl("/dashboard", true) // Куди перенаправити після успішного входу
                        .permitAll()
                )
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") // Куди перенаправити після виходу
                        .permitAll()
                )
                // Вимикаємо CSRF для спрощення роботи з API завантаження (для навчального проєкту це ок)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    // Бін для шифрування паролів (щоб не зберігати їх як простий текст)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Бін, який вчить Spring Security шукати користувачів у нашій базі даних
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}