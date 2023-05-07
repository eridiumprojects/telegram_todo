package com.example.telegram.service;

import com.example.telegram.model.constant.MessagePool;
import com.example.telegram.model.dto.BotChange;
import com.example.telegram.model.dto.request.LoginRequest;
import com.example.telegram.model.dto.request.RefreshRequest;
import com.example.telegram.model.enums.BotState;
import com.example.telegram.rest.CoreApiClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.example.telegram.model.constant.MessagePool.LOGIN_IN_ACCOUNT_WITH_MESSAGE;

@Service
@Getter
@Setter
@Log4j2
public class AuthService {
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final RMapCache<Long, String> accessTokensMap;
    private final RMapCache<Long, String> refreshTokensMap;
    private final CoreApiClient coreApiClient;

    public AuthService(
            @Value("${storage.access-token}") String accessTokenStorage,
            @Value("${storage.refresh-token}") String refreshTokenStorage,
            @Value("${security.ttl.access}") Duration accessTokenTtl,
            @Value("${security.ttl.refresh}") Duration refreshTokenTtl,
            RedissonClient redissonClient,
            CoreApiClient coreApiClient
    ) {
        this.coreApiClient = coreApiClient;

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
            var response = coreApiClient.postForJwt(user);
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
            log.info("Can't authorize user {}", userId);
            return false;
        }
    }

    public boolean refreshToken(Long userId) {
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(refreshTokensMap.get(userId));
        try {
            var response = coreApiClient.postForRefresh(refreshRequest);
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
            log.warn("Invalid refresh token. userId {}", userId);
            return false;
        }
    }

    public BotChange signOut(Long messageChatId, boolean forced) {
        accessTokensMap.remove(messageChatId);
        refreshTokensMap.remove(messageChatId);

        if (forced) {
            log.warn("User session has expired");
        }
        log.debug("Access map deleted the user id and token successfully");
        log.debug("Refresh map deleted the user id and token successfully");

        var messageBeginning = forced ? MessagePool.SESSION_EXPIRED : MessagePool.SIGNOUT_MESSAGE;
        return new BotChange(
                BotState.BASE,
                messageBeginning + "\n" + LOGIN_IN_ACCOUNT_WITH_MESSAGE);
    }

    public String getUserAccessToken(Long userId) {
        return accessTokensMap.get(userId);
    }
}
