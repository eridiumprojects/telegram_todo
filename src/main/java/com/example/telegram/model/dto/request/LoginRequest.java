package com.example.telegram.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginRequest {
    private String username;
    private String password;
    private UUID deviceToken;
}
