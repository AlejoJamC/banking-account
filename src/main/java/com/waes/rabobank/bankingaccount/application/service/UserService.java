package com.waes.rabobank.bankingaccount.application.service;

import com.waes.rabobank.bankingaccount.application.dto.UserResponseDTO;
import com.waes.rabobank.bankingaccount.domain.model.User;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.UserRepository;
import com.waes.rabobank.bankingaccount.shared.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponseDTO(
                        user.getId().toString(),
                        user.getFullName(),
                        user.getEmail()))
                .toList();
    }

    public UserResponseDTO searchUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be blank");
        }

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UserNotFoundException(email));  // ← Llama al constructor String

        return new UserResponseDTO(
                user.getId().toString(),
                user.getFullName(),
                user.getEmail()
        );
    }
}
