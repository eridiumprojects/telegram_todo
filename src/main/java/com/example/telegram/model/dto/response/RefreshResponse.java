package com.example.telegram.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RefreshResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
}