package com.vectoros.server.user.service;

import com.vectoros.server.user.entity.User;
import com.vectoros.server.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        return userRepository.findByTelegramId(telegramId);
    }

}
