package com.example.telegram.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
//@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtResponse {
    private String accessToken;
    private String type = "Bearer";
    private Long id;
    private String refreshToken;
    private String username;
    private String email;
    private List<String> roles;
//    @JsonProperty
//    private Long timestamp;
//    @JsonProperty
//    private String messageError;
//    @JsonProperty
//    private String cause;
}
