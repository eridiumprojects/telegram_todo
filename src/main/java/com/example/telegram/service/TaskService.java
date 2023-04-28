package com.example.telegram.service;

import com.example.telegram.model.dto.request.CreateTaskRequest;
import com.example.telegram.model.dto.response.TaskInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Getter
@Setter
@Slf4j
public class TaskService {
    private int statusCode;
    private final RestTemplate restTemplate;

    public TaskService(RestTemplateBuilder restTemplateBuilder,
                       @Value("${backend.url}") String baseApiUrl) {
        this.restTemplate = restTemplateBuilder.rootUri(baseApiUrl).build();
    }

    public String getTaskList(String token) {
        var headers = new LinkedMultiValueMap<String, String>();
        headers.add("Authorization", "Bearer " + token);
        var requestEntity = new HttpEntity<>(headers);
        
        try {
            var result = restTemplate.exchange(
                    "/task/list",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<TaskInfo>>() {}).getBody();

            if (result == null || result.size() == 0) {
                return "[]";
            }

            AtomicInteger numeration = new AtomicInteger(0);
            var adaptedToUser = result.stream()
                    .map(TaskInfo::getData)
                    .map(o -> {
                        numeration.getAndIncrement();
                        return numeration + ". " + o + "\n";
                    })
                    .toList();

            return StringUtils.join(adaptedToUser);
        } catch (RestClientException e) {
            log.warn("User couldn't get task list. Using default response");
            return "[]";
        }
    }

    public boolean createTask(String token, String data) {
        var headers = new LinkedMultiValueMap<String, String>();
        var body = new CreateTaskRequest(data, null);
        headers.add("Authorization", "Bearer " + token);
        var requestEntity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(
                    "/task/create",
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<TaskInfo>() {});

            return true;
        } catch (RestClientException e) {
            log.warn("User couldn't create task.");
            return false;
        }
    }
}
