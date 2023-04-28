package com.example.telegram.model.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginRequest {
    private Long userId;
    private String username;
    private String password;
    @JsonIgnore
    private UUID deviceToken;
}
