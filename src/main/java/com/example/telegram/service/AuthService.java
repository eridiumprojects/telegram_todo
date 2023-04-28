package com.example.telegram.service;

import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.request.RefreshRequest;
import com.example.telegram.model.dto.response.JwtResponse;
import com.example.telegram.model.dto.response.RefreshResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Getter
@Setter
@Log4j2
public class AuthService {
    private int statusCode;
    private final RestTemplate restTemplate;
    private final String REFRESH_URL = "/auth/refresh";
    private final String AUTH_URL = "/auth/signin";

    public AuthService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("backend.url") String baseApiUrl
    ) {
        this.restTemplate = restTemplateBuilder.rootUri(baseApiUrl).build();
    }

    public JwtResponse sendRequestToAuthService(LoginRequest user) {
        try {
            ResponseEntity<JwtResponse> responseEntity = restTemplate.exchange(
                    AUTH_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(user),
                    JwtResponse.class
            );
            setStatusCode(responseEntity.getStatusCode().value());
            return responseEntity.getBody();
        } catch (RestClientException e) {
            setStatusCode(HttpStatus.UNAUTHORIZED.value());
            return null;
        }
    }

    public RefreshResponse refreshToken(String refreshToken) {
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(refreshToken);
        try {
            ResponseEntity<RefreshResponse> responseEntity = restTemplate.exchange(
                    REFRESH_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(refreshRequest),
                    RefreshResponse.class
            );
            log.info("Access and refresh token has been updated");

            setStatusCode(responseEntity.getStatusCode().value());
            return responseEntity.getBody();
        } catch (RestClientException e) {
            setStatusCode(HttpStatus.UNAUTHORIZED.value());
            log.warn("Refresh token has been expired");
            return null;
        }
    }
}
