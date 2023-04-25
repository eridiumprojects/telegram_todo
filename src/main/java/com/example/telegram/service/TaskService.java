package com.example.telegram.service;

import com.example.telegram.model.dto.request.TaskRequest;
import com.example.telegram.util.RequestBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.springframework.http.HttpStatus;
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
public class TaskService {
    private int statusCode;
    private final RequestBuilder requestBuilder;

    public void sendCreateTaskRequest(String token, TaskRequest taskRequest) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = requestBuilder.postCreatingHttpResponse(
                     httpclient,
                     taskRequest,
                     "/task/create",
                     token)) {
            setStatusCode(response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
        }
    }

    public String sendShowTasksRequest(String token) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = requestBuilder.getCreatingHttpResponse(
                     httpclient,
                     "/task/list",
                     token)) {
            setStatusCode(response.getStatusLine().getStatusCode());
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
