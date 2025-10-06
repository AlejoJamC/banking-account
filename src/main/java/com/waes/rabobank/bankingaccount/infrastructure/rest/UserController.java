package com.waes.rabobank.bankingaccount.infrastructure.rest;

import com.waes.rabobank.bankingaccount.application.dto.UserResponseDTO;
import com.waes.rabobank.bankingaccount.application.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponseDTO> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/search")
    public UserResponseDTO searchUsers(@RequestParam String email) {
        return userService.searchUserByEmail(email);
    }
}
