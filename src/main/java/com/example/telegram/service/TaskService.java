package com.example.telegram.service;

import com.example.telegram.model.dto.request.TaskRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Getter
@Setter
public class TaskService {
    private int statusCode;
    private final RestTemplate restTemplate;

    public TaskService(RestTemplateBuilder restTemplateBuilder,
                       @Value("backend.url") String baseApiUrl) {
        this.restTemplate = restTemplateBuilder.rootUri(baseApiUrl).build();
    }

    public String sendRequestToTaskService(String token, TaskRequest taskRequest, boolean option) throws IOException {
        String path = option ? "/task/create" : "/task/list";

        CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = requestBuilder.createResponse(
                     httpclient,
                     taskRequest,
                     path,
                     token,
                     option)) {

            setStatusCode(response.getStatusLine().getStatusCode());
            if (option) {
                EntityUtils.consume(response.getEntity());
                return null;
            }
            return new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                    .lines()
                    .collect(Collectors.joining());
        }

    }

    public String tasksFromJsonString(String jsonString) {
        JSONArray jsonArray = new JSONArray(jsonString);
        List<String> tasks = IntStream
                .range(0, jsonArray.length())
                .mapToObj(i -> jsonArray.getJSONObject(i).getString("data"))
                .toList();
        return IntStream
                .range(0, tasks.size())
                .mapToObj(i -> (i + 1) + ". " + tasks.get(i) + "\n")
                .collect(Collectors.joining());
    }
}
