package com.vectoros.server.user.service;

import com.vectoros.server.user.entity.User;
import com.vectoros.server.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    public User findOrCreate(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setTelegramId(telegramId);
                    return userRepository.save(user);
                });
    }

    public Optional<User> findByTelegramId(Long telegramId) {
        log.debug("Searching for user with telegramId: {}", telegramId);
        Optional<User> user = userRepository.findByTelegramId(telegramId);
        if (user.isPresent()) {
            log.debug("User found: id={}, telegramId={}", user.get().getId(), user.get().getTelegramId());
        } else {
            log.warn("User not found with telegramId: {}", telegramId);
            // Для диагностики: проверим, сколько всего пользователей в БД
            long totalUsers = userRepository.count();
            log.debug("Total users in database: {}", totalUsers);
        }
        return user;
    }

}
