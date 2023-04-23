package com.example.telegram.service;

import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.response.JwtResponse;
import com.example.telegram.util.RequestBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Getter
@Setter
@Log4j2
public class AuthService {
    private int statusCode;
    private final RequestBuilder requestBuilder;

    public JwtResponse jwtFromJsonString(String jsonString) throws JsonProcessingException {
        log.info("object mapper method started");
        ObjectMapper objectMapper = new ObjectMapper();
        log.info("object mapper object created");
        log.info("jsonString = " + jsonString);
        return objectMapper.readValue(jsonString, JwtResponse.class);
    }

//    public String sendSignInRequest(LoginRequest user) throws IOException {
//        log.info("method sendSignInRequest started");
//        try (
//                CloseableHttpClient httpclient = HttpClients.createDefault();
//                CloseableHttpResponse response = requestBuilder.postCreatingHttpResponse(
//                        httpclient,
//                        user,
//                        "/auth/signin",
//                        null)) {
//            setStatusCode(response.getStatusLine().getStatusCode());
//            log.info("method sendSignInRequest finished");
//            return new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
//                    .lines()
//                    .collect(Collectors.joining());
//        }
//    }

    public JwtResponse sendSignInRequest(LoginRequest user) throws IOException {
        log.info("method sendSignInRequest started");

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(user, headers);

        ResponseEntity<JwtResponse> responseEntity;
        try {
            responseEntity = restTemplate.exchange(
                    "http://localhost:8080/api/auth/signin",
                    HttpMethod.POST,
                    requestEntity,
                    JwtResponse.class
            );
            setStatusCode(responseEntity.getStatusCode());
        } catch (RestClientException e) {
            //it will be caused if response entity returns with not 200 HttpStatus
        }
        //don't use deprecated methods
        setStatusCode(responseEntity.getStatusCodeValue());

        log.info("method sendSignInRequest finished");

        return responseEntity.getBody();
    }



//    public String sendCurrentUserRequest(String token) throws IOException {
//        try (CloseableHttpClient httpclient = HttpClients.createDefault();
//             CloseableHttpResponse response = requestBuilder.getCreatingHttpResponse(
//                     httpclient,
//                     "/user/current",
//                     token)) {
//            return new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
//                    .lines()
//                    .collect(Collectors.joining());
//        }
//    }
//
//    public String credentialsFromJsonString(String jsonString) {
//        JSONArray jsonArray = new JSONArray(jsonString);
//        List<String> credentials = IntStream
//                .range(0, jsonArray.length())
//                .mapToObj(i -> jsonArray.getJSONObject(i).getString("data"))
//                .toList();
//        return IntStream
//                .range(0, tasks.size())
//                .mapToObj(i -> (i + 1) + ". " + tasks.get(i) + "\n")
//                .collect(Collectors.joining());
//    }
}
