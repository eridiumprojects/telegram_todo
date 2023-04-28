package com.example.telegram.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
public class RefreshRequest {
    private String refreshToken;
}
