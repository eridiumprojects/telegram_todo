package com.example.telegram.service;

import com.example.telegram.model.constant.MessagePool;
import com.example.telegram.model.dto.BotChange;
import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.request.RefreshRequest;
import com.example.telegram.model.dto.response.JwtResponse;
import com.example.telegram.model.dto.response.RefreshResponse;
import com.example.telegram.model.enums.BotState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.example.telegram.model.constant.MessagePool.LOGIN_IN_ACCOUNT_WITH_MESSAGE;

@Service
@Getter
@Setter
@Log4j2
public class AuthService {
    private final RestTemplate restTemplate;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final RMapCache<Long, String> accessTokensMap;
    private final RMapCache<Long, String> refreshTokensMap;

    public AuthService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${backend.url}") String baseApiUrl,
            @Value("${storage.access-token}") String accessTokenStorage,
            @Value("${storage.refresh-token}") String refreshTokenStorage,
            @Value("${security.ttl.access}") Duration accessTokenTtl,
            @Value("${security.ttl.refresh}") Duration refreshTokenTtl,
            RedissonClient redissonClient
    ) {
        this.restTemplate = restTemplateBuilder.rootUri(baseApiUrl).build();

        this.accessTokensMap = redissonClient.getMapCache(accessTokenStorage);
        this.refreshTokensMap = redissonClient.getMapCache(refreshTokenStorage);

        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public boolean checkForLogin(Long userId) {
        if (accessTokensMap.containsKey(userId)) {
            log.info("Valid access token exists");
            return true;
        } else if (refreshTokensMap.containsKey(userId)) {
            log.info("Valid refresh token exists");
            return refreshToken(userId);
        }
        return false;
    }

    public boolean loginUser(Long userId, LoginRequest user) {
        try {
            var response = restTemplate.exchange(
                    "/auth/signin",
                    HttpMethod.POST,
                    new HttpEntity<>(user),
                    JwtResponse.class
            ).getBody();
            if (response == null) {
                throw new RestClientException("API changed behavior");
            }
            accessTokensMap.put(
                    userId,
                    response.getAccessToken(),
                    accessTokenTtl.toMinutes(),
                    TimeUnit.MINUTES);
            refreshTokensMap.put(
                    userId,
                    response.getRefreshToken(),
                    refreshTokenTtl.toMinutes(),
                    TimeUnit.MINUTES);
            return true;
        } catch (RestClientException e) {
            log.info("Can't authorize user {} timestamp {}", userId, Instant.now());
            return false;
        }
    }

    public boolean refreshToken(Long userId) {
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(refreshTokensMap.get(userId));
        try {
            var response = restTemplate.exchange(
                    "/auth/refresh",
                    HttpMethod.POST,
                    new HttpEntity<>(refreshRequest),
                    RefreshResponse.class
            ).getBody();
            if (response == null) {
                log.info("Incorrect API behavior on refresh");
                return false;
            }
            accessTokensMap.put(
                    userId,
                    response.getAccessToken(),
                    accessTokenTtl.toMinutes(),
                    TimeUnit.MINUTES);
            refreshTokensMap.put(
                    userId,
                    response.getRefreshToken(),
                    refreshTokenTtl.toMinutes(),
                    TimeUnit.MINUTES);
            log.info("Access and refresh token has been updated");
            return true;
        } catch (RestClientException e) {
            log.warn("Invalid refresh token. userId {} timestamp {}", userId, Instant.now());
            return false;
        }
    }

    public BotChange signOut(Long messageChatId, boolean forced) {
        accessTokensMap.remove(messageChatId);
        refreshTokensMap.remove(messageChatId);

        log.warn("User session has expired");
        log.info("Access map deleted the user id and token successfully");
        log.info("Refresh map deleted the user id and token successfully");

        var messageBeginning = forced ? MessagePool.SESSION_EXPIRED : MessagePool.SIGNOUT_MESSAGE;
        return new BotChange(
                BotState.BASE,
                messageBeginning + "\n" + LOGIN_IN_ACCOUNT_WITH_MESSAGE);
    }

    public String getUserAccessToken(Long userId) {
        return accessTokensMap.get(userId);
    }
}
