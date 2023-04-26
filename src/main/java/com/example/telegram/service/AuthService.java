package com.example.telegram.service;

import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.request.RefreshRequest;
import com.example.telegram.model.dto.response.JwtResponse;
import com.example.telegram.model.dto.response.RefreshResponse;
import com.example.telegram.util.RequestBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Getter
@Setter
@Log4j2
public class AuthService {
    private int statusCode;
    private final RequestBuilder requestBuilder;
    private static final String REFRESH_URL = "http://localhost:8080/api/auth/refresh";
    private static final String AUTH_URL = "http://localhost:8080/api/auth/signin";

    public JwtResponse sendRequestToAuthService(LoginRequest user) {
        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(user, headers);

            ResponseEntity<JwtResponse> responseEntity = restTemplate.exchange(
                    AUTH_URL,
                    HttpMethod.POST,
                    requestEntity,
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
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<RefreshResponse> responseEntity = restTemplate.exchange(
                    REFRESH_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(
                            refreshRequest
                    ),
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