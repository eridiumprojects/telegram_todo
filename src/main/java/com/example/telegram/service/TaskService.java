package com.example.telegram.service;

import com.example.telegram.model.dto.request.CreateTaskRequest;
import com.example.telegram.model.dto.response.TaskInfo;
import com.example.telegram.model.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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

            var taskMap = result.stream()
                    .collect(Collectors.groupingBy(
                            TaskInfo::getStatus,
                            Collectors.mapping(TaskInfo::getData, toList())))
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(a -> TaskStatus.priorities.get(a.getKey())))
                    .toList();

            var builder = new StringBuilder();
            int numeration = 1;

            for (var el : taskMap) {
                builder.append('\n');
                builder.append("[").append(el.getKey().getStatus()).append("]").append("\n");
                for (var subEl : el.getValue()) {
                    builder.append(numeration).append(". ").append(subEl).append("\n");
                    numeration++;
                }
            }

            return builder.toString();
        } catch (RestClientException e) {
            log.warn("User couldn't get task list. Will try to refresh if didn't");
            return null;
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
            log.warn("User couldn't create task. Will try to refresh if didn't");
            return false;
        }
    }
}
