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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public String sendSignInRequest(LoginRequest user) throws IOException {
        log.info("method sendSignInRequest started");
        try (
                CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = requestBuilder.postCreatingHttpResponse(
                        httpclient,
                        user,
                        "/auth/signin",
                        null)) {
            setStatusCode(response.getStatusLine().getStatusCode());
            log.info("method sendSignInRequest finished");
            return new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                    .lines()
                    .collect(Collectors.joining());
        }
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
