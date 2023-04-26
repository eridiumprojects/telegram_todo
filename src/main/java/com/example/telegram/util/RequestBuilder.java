package com.example.telegram.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RequestBuilder {
    private String URL = "http://localhost:8080/api";
    private final ObjectMapper objectMapper;

    public CloseableHttpResponse createResponse(
            CloseableHttpClient client,
            Object object,
            String path,
            String token,
            boolean option) throws IOException {

        Charset charset = StandardCharsets.UTF_8;
        ContentType contentType = ContentType.create("application/json", charset);

        if (option) {
            HttpPost httpPost = new HttpPost(URL + path);

            String json = objectMapper.writeValueAsString(object);

            StringEntity entity = new StringEntity(json, contentType);
            entity.setContentType("application/json");

            httpPost.setEntity(entity);

            if (token != null) {
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            return client.execute(httpPost);
        }

        HttpGet httpGet = new HttpGet(URL + path);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        return client.execute(httpGet);
    }
}