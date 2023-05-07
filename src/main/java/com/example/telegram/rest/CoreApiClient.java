package com.example.telegram.rest;

import com.example.telegram.model.dto.request.CreateTaskRequest;
import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.request.RefreshRequest;
import com.example.telegram.model.dto.response.JwtResponse;
import com.example.telegram.model.dto.response.RefreshResponse;
import com.example.telegram.model.dto.response.TaskInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class CoreApiClient {
    private final RestTemplate restTemplate;


    public CoreApiClient(
            @Value("${backend.url}") String baseApiUrl,
            RestTemplateBuilder restTemplate
    ) {
        this.restTemplate = restTemplate.rootUri(baseApiUrl).build();
//        this.restTemplate.exchange(
//                "/auth/signin",
//                HttpMethod.POST,
//                new HttpEntity<>(new LoginRequest("boba", "boba", UUID.randomUUID())),
//                JwtResponse.class);
    }

    public JwtResponse postForJwt(LoginRequest loginRequest) {
        var result = post(
                "/auth/signin",
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<JwtResponse>() {});
        if (result.getBody() == null) {
            log.warn("API changed behavior");
            throw new RestClientException("API changed behavior");
        }
        return (JwtResponse) result.getBody();
    }

    public RefreshResponse postForRefresh(RefreshRequest refreshRequest) {
        var result = post(
                "/auth/refresh",
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<RefreshResponse>() {});
        if (result.getBody() == null) {
            log.warn("API changed behavior");
            throw new RestClientException("API changed behavior");
        }
        return (RefreshResponse) result.getBody();
    }

    public List<TaskInfo> getForTaskList(String authToken) {
        var headers = new LinkedMultiValueMap<String, String>();
        headers.add("Authorization", "Bearer " + authToken);

        return (List<TaskInfo>) get(
                "/task/list",
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<TaskInfo>>() {}).getBody();
    }

    public void postForTaskCreate(String token, String data) {
        var headers = new LinkedMultiValueMap<String, String>();
        var body = new CreateTaskRequest(data, null);
        headers.add("Authorization", "Bearer " + token);
        var requestEntity = new HttpEntity<>(body, headers);

        post("/task/create",
                requestEntity,
                new ParameterizedTypeReference<TaskInfo>(){});
    }

    public ResponseEntity<?> post(String uri, HttpEntity<?> request, ParameterizedTypeReference<?> responseType) {
         return restTemplate.exchange(
                 uri,
                 HttpMethod.POST,
                 request,
                 responseType
         );
    }

    public ResponseEntity<?> get(String uri, HttpEntity<?> request, ParameterizedTypeReference<?> responseType) {
        return restTemplate.exchange(
                uri,
                HttpMethod.GET,
                request,
                responseType
        );
    }
}
