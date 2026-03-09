package com.sprint.mission.discodeit.common.security.jwt;

import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.sse.SseService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final JwtRegistry jwtRegistry;
    private final JwtTokenProvider jwtTokenProvider;
    private final CacheManager cacheManager;
    private final UserRepository userRepository;
    private final JwtOnlineChecker jwtOnlineChecker;
    private final SseService sseService;
    private final UserMapper userMapper;

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        String refreshToken = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            refreshToken = Arrays.stream(cookies)
                    .filter(c -> "REFRESH_TOKEN".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        refreshTokenCookieManager.clear(response);

        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        if (!jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
            return;
        }

        UUID userId;

        try {
            userId = UUID.fromString(jwtTokenProvider.getSubject(refreshToken));
        } catch (Exception e) {
            return;
        }

        jwtRegistry.invalidateJwtInformationByUserId(userId);

        Cache cache = cacheManager.getCache("userListCache");
        if (cache != null) {
            cache.evict("all");
        }

        userRepository.findById(userId).ifPresent(user -> {
            boolean online = jwtOnlineChecker.isOnline(userId);
            sseService.broadcast("users.updated", userMapper.toDto(user, online));
        });
    }
}
