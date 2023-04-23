package com.example.telegram.service;

import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.response.JwtResponse;
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

    public JwtResponse sendSignInRequest(LoginRequest user) {
        try {
            log.info("method sendSignInRequest started");

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(user, headers);

            ResponseEntity<JwtResponse> responseEntity = restTemplate.exchange(
                    "http://localhost:8080/api/auth/signin",
                    HttpMethod.POST,
                    requestEntity,
                    JwtResponse.class
            );
            setStatusCode(responseEntity.getStatusCode().value());
            log.info("method sendSignInRequest finished");

            return responseEntity.getBody();
        } catch (RestClientException e) {
            setStatusCode(HttpStatus.UNAUTHORIZED.value());
            return null;
        }
    }
}

