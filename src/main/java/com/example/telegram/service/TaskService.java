package com.example.telegram.service;

import com.example.telegram.model.dto.response.TaskInfo;
import com.example.telegram.model.enums.TaskStatus;
import com.example.telegram.rest.CoreApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Comparator;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final CoreApiClient coreApiClient;

    public String getTaskList(String token) {
        try {
            var result = coreApiClient.getForTaskList(token);

            var taskMap = result.stream()
                    .collect(Collectors.groupingBy(
                            TaskInfo::getStatus,
                            Collectors.mapping(TaskInfo::getData, toList())))
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(a -> TaskStatus.priorities.get(a.getKey())))
                    .filter(o -> !o.getValue().isEmpty())
                    .toList();

            var builder = new StringBuilder();
            int numeration = 1;

            for (var el : taskMap) {
                builder.append("\n");
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
        try {
            coreApiClient.postForTaskCreate(token, data);
            return true;
        } catch (RestClientException e) {
            log.warn("User couldn't create task. Will try to refresh if didn't");
            return false;
        }
    }
}
