package org.unihubworkshop.apigateway.filters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisSlidingWindowRateLimitFilter implements GlobalFilter, Ordered {

    // Đã tối ưu Lua Script: Đếm trước khi thêm vào ZSET để chống rác RAM Redis
    private static final String SLIDING_WINDOW_LUA = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1] - ARGV[2])
            local requestCount = redis.call('ZCARD', KEYS[1])
            if requestCount >= tonumber(ARGV[3]) then
                return 0
            end
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return {1, requestCount + 1}
            """;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List> slidingWindowScript;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.max-requests:3000}")
    private int maxRequests;

    @Value("${rate-limit.window-seconds:10}")
    private long windowSeconds;

    @Value("${rate-limit.key-prefix:unihub:gateway:ratelimit}")
    private String keyPrefix;

    public RedisSlidingWindowRateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.slidingWindowScript = RedisScript.of(SLIDING_WINDOW_LUA, List.class);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        String routeKey = extractRouteKey(path, method);
        String principalKey = extractPrincipalKey(exchange);
        String key = keyPrefix + ":" + routeKey + ":" + principalKey;

        int effectiveMaxRequests = this.maxRequests;
        long effectiveWindowMillis = this.windowSeconds * 1000;

//        if ("action:registration".equals(routeKey)) {
//            effectiveMaxRequests = 5;
//            effectiveWindowMillis = 10000;
//        }

        long nowMillis = System.currentTimeMillis();
        String requestMember = nowMillis + "-" + UUID.randomUUID();

        // Tính toán thời gian retry động dựa trên route
        long retryAfterSeconds = effectiveWindowMillis / 1000;

        return redisTemplate.execute(
                        slidingWindowScript,
                        List.of(key),
                        String.valueOf(nowMillis),
                        String.valueOf(effectiveWindowMillis), // ARGV[2]
                        String.valueOf(effectiveMaxRequests),  // ARGV[3]
                        requestMember
                )
                .next()
                .defaultIfEmpty(List.of(1L, 0L))
                .flatMap(result -> {
                    Long allowed = ((Number) result.get(0)).longValue();
                    Long currentCount = ((Number) result.get(1)).longValue();

                    System.out.println(
                            "[RATE LIMIT] key=" + key +
                                    " currentCount=" + currentCount +
                                    " limit=" + effectiveMaxRequests
                    );

                    if (allowed == 1L) {
                        return chain.filter(exchange);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    // Đã sửa header dùng biến động thay vì biến global
                    exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));
                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(throwable -> chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -110;
    }

    private String extractRouteKey(String path, HttpMethod method) {
        if (path == null || path.isBlank()) {
            return "root";
        }

        if (path.endsWith("/tickets") && HttpMethod.POST.equals(method)) {
            return "action:registration";
        }

        String normalized = path.startsWith("/api/") ? path.substring(5) :
                (path.startsWith("/") ? path.substring(1) : path);

        String[] segments = normalized.split("/");
        if (segments.length == 0 || segments[0].isBlank()) {
            return "root";
        }

        if (segments.length > 1) {
            return segments[0] + ":" + segments[1];
        }
        return segments[0];
    }

    private String extractPrincipalKey(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");


        if (userId != null && !userId.isBlank()) {
            return "u:" + userId;
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            return "t:" + sha256(authHeader);
        }

        String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(address -> address.getAddress().getHostAddress())
                .orElse("unknown");
        return "ip:" + ip;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
//package org.unihubworkshop.apigateway.filters;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.data.redis.core.script.RedisScript;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.HexFormat;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//@Component
//public class RedisSlidingWindowRateLimitFilter implements GlobalFilter, Ordered {
//
//    private static final String SLIDING_WINDOW_LUA = """
//            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[2])
//            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
//            local requestCount = redis.call('ZCARD', KEYS[1])
//            redis.call('PEXPIRE', KEYS[1], ARGV[2])
//            if requestCount > tonumber(ARGV[3]) then
//                return 0
//            end
//            return 1
//            """;
//
//    private final ReactiveStringRedisTemplate redisTemplate;
//    private final RedisScript<Long> slidingWindowScript;
//
//    @Value("${rate-limit.enabled:true}")
//    private boolean enabled;
//
//    @Value("${rate-limit.max-requests:3000}")
//    private int maxRequests;
//
//    @Value("${rate-limit.window-seconds:10}")
//    private long windowSeconds;
//
//    @Value("${rate-limit.key-prefix:unihub:gateway:ratelimit}")
//    private String keyPrefix;
//
//    public RedisSlidingWindowRateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
//        this.redisTemplate = redisTemplate;
//        this.slidingWindowScript = RedisScript.of(SLIDING_WINDOW_LUA, Long.class);
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
//        if (!enabled) {
//            return chain.filter(exchange);
//        }
//
//        String path = exchange.getRequest().getPath().value();
//        HttpMethod method = exchange.getRequest().getMethod();
//        String routeKey = extractRouteKey(path, method);
//        String principalKey = extractPrincipalKey(exchange);
//        String key = keyPrefix + ":" + routeKey + ":" + principalKey;
//
//        int effectiveMaxRequests = this.maxRequests; // Mặc định là 60
//        long effectiveWindowMillis = this.windowSeconds * 1000; // Mặc định là 10s
//
//        if ("action:registration".equals(routeKey)) {
//            effectiveMaxRequests = 5;
//            effectiveWindowMillis = 10000;
//        }
//
//        long nowMillis = System.currentTimeMillis();
//        String requestMember = nowMillis + "-" + UUID.randomUUID();
//
//        return redisTemplate.execute(
//                        slidingWindowScript,
//                        List.of(key),
//                        String.valueOf(nowMillis),
//                        String.valueOf(effectiveWindowMillis),
//                        String.valueOf(effectiveMaxRequests),
//                        requestMember
//                )
//                .next()
//                .defaultIfEmpty(1L)
//                .flatMap(result -> {
//                    if (result != null && result == 1L) {
//                        return chain.filter(exchange);
//                    }
//                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
//                    exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(windowSeconds));
//                    return exchange.getResponse().setComplete();
//                })
//                .onErrorResume(throwable -> chain.filter(exchange));
//    }
//
//    @Override
//    public int getOrder() {
//        return -110;
//    }
//
//    private String extractRouteKey(String path, HttpMethod method) {
//        if (path == null || path.isBlank()) {
//            return "root";
//        }
//
//        if (path.endsWith("/tickets") && HttpMethod.POST.equals(method)) {
//            return "action:registration";
//        }
//
//        String normalized = path.startsWith("/api/") ? path.substring(5) :
//                (path.startsWith("/") ? path.substring(1) : path);
//
//        String[] segments = normalized.split("/");
//        if (segments.length == 0 || segments[0].isBlank()) {
//            return "root";
//        }
//
//        if (segments.length > 1) {
//            return segments[0] + ":" + segments[1];
//        }
//        return segments[0];
//    }
//
//    private String extractPrincipalKey(ServerWebExchange exchange) {
//        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
//        if (userId != null && !userId.isBlank()) {
//            return "u:" + userId;
//        }
//
//        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
//        if (authHeader != null && !authHeader.isBlank()) {
//            return "t:" + sha256(authHeader);
//        }
//
//        String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
//                .map(address -> address.getAddress().getHostAddress())
//                .orElse("unknown");
//        return "ip:" + ip;
//    }
//
//    private String sha256(String value) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
//            return HexFormat.of().formatHex(hash);
//        } catch (NoSuchAlgorithmException e) {
//            return Integer.toHexString(value.hashCode());
//        }
//    }
//}


//package org.unihubworkshop.apigateway.filters;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.data.redis.core.script.RedisScript;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.HexFormat;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//@Component
//public class RedisSlidingWindowRateLimitFilter implements GlobalFilter, Ordered {
//
//    // Đã tối ưu Lua Script: Đếm trước khi thêm vào ZSET để chống rác RAM Redis
//    private static final String SLIDING_WINDOW_LUA = """
//            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1] - ARGV[2])
//            local requestCount = redis.call('ZCARD', KEYS[1])
//            if requestCount >= tonumber(ARGV[3]) then
//                return 0
//            end
//            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
//            redis.call('PEXPIRE', KEYS[1], ARGV[2])
//            return 1
//            """;
//
//    private final ReactiveStringRedisTemplate redisTemplate;
//    private final RedisScript<Long> slidingWindowScript;
//
//    @Value("${rate-limit.enabled:true}")
//    private boolean enabled;
//
//    @Value("${rate-limit.max-requests:3000}")
//    private int maxRequests;
//
//    @Value("${rate-limit.window-seconds:10}")
//    private long windowSeconds;
//
//    @Value("${rate-limit.key-prefix:unihub:gateway:ratelimit}")
//    private String keyPrefix;
//
//    public RedisSlidingWindowRateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
//        this.redisTemplate = redisTemplate;
//        this.slidingWindowScript = RedisScript.of(SLIDING_WINDOW_LUA, Long.class);
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
//        if (!enabled) {
//            return chain.filter(exchange);
//        }
//
//        String path = exchange.getRequest().getPath().value();
//        HttpMethod method = exchange.getRequest().getMethod();
//        String routeKey = extractRouteKey(path, method);
//        String principalKey = extractPrincipalKey(exchange);
//        String key = keyPrefix + ":" + routeKey + ":" + principalKey;
//
//        int effectiveMaxRequests = this.maxRequests;
//        long effectiveWindowMillis = this.windowSeconds * 1000;
//
////        if ("action:registration".equals(routeKey)) {
////            effectiveMaxRequests = 5;
////            effectiveWindowMillis = 10000;
////        }
//
//        long nowMillis = System.currentTimeMillis();
//        String requestMember = nowMillis + "-" + UUID.randomUUID();
//
//        // Tính toán thời gian retry động dựa trên route
//        long retryAfterSeconds = effectiveWindowMillis / 1000;
//
//        return redisTemplate.execute(
//                        slidingWindowScript,
//                        List.of(key),
//                        String.valueOf(nowMillis),
//                        String.valueOf(effectiveWindowMillis), // ARGV[2]
//                        String.valueOf(effectiveMaxRequests),  // ARGV[3]
//                        requestMember
//                )
//                .next()
//                .defaultIfEmpty(1L)
//                .flatMap(result -> {
//                    if (result != null && result == 1L) {
//                        return chain.filter(exchange);
//                    }
//                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
//                    // Đã sửa header dùng biến động thay vì biến global
//                    exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));
//                    return exchange.getResponse().setComplete();
//                })
//                .onErrorResume(throwable -> chain.filter(exchange));
//    }
//
//    @Override
//    public int getOrder() {
//        return -110;
//    }
//
//    private String extractRouteKey(String path, HttpMethod method) {
//        if (path == null || path.isBlank()) {
//            return "root";
//        }
//
//        if (path.endsWith("/tickets") && HttpMethod.POST.equals(method)) {
//            return "action:registration";
//        }
//
//        String normalized = path.startsWith("/api/") ? path.substring(5) :
//                (path.startsWith("/") ? path.substring(1) : path);
//
//        String[] segments = normalized.split("/");
//        if (segments.length == 0 || segments[0].isBlank()) {
//            return "root";
//        }
//
//        if (segments.length > 1) {
//            return segments[0] + ":" + segments[1];
//        }
//        return segments[0];
//    }
//
//    private String extractPrincipalKey(ServerWebExchange exchange) {
//        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
//
//
////        if (userId != null && !userId.isBlank()) {
////            return "u:" + userId;
////        }
////
////        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
////        if (authHeader != null && !authHeader.isBlank()) {
////            return "t:" + sha256(authHeader);
////        }
//
//        String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
//                .map(address -> address.getAddress().getHostAddress())
//                .orElse("unknown");
//        return "ip:" + ip;
//    }
//
//    private String sha256(String value) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
//            return HexFormat.of().formatHex(hash);
//        } catch (NoSuchAlgorithmException e) {
//            return Integer.toHexString(value.hashCode());
//        }
//    }
//}
////package org.unihubworkshop.apigateway.filters;
////
////import org.springframework.beans.factory.annotation.Value;
////import org.springframework.cloud.gateway.filter.GlobalFilter;
////import org.springframework.core.Ordered;
////import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
////import org.springframework.data.redis.core.script.RedisScript;
////import org.springframework.http.HttpMethod;
////import org.springframework.http.HttpStatus;
////import org.springframework.stereotype.Component;
////import org.springframework.web.server.ServerWebExchange;
////import reactor.core.publisher.Mono;
////
////import java.nio.charset.StandardCharsets;
////import java.security.MessageDigest;
////import java.security.NoSuchAlgorithmException;
////import java.util.HexFormat;
////import java.util.List;
////import java.util.Optional;
////import java.util.UUID;
////
////@Component
////public class RedisSlidingWindowRateLimitFilter implements GlobalFilter, Ordered {
////
////    private static final String SLIDING_WINDOW_LUA = """
////            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[2])
////            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
////            local requestCount = redis.call('ZCARD', KEYS[1])
////            redis.call('PEXPIRE', KEYS[1], ARGV[2])
////            if requestCount > tonumber(ARGV[3]) then
////                return 0
////            end
////            return 1
////            """;
////
////    private final ReactiveStringRedisTemplate redisTemplate;
////    private final RedisScript<Long> slidingWindowScript;
////
////    @Value("${rate-limit.enabled:true}")
////    private boolean enabled;
////
////    @Value("${rate-limit.max-requests:3000}")
////    private int maxRequests;
////
////    @Value("${rate-limit.window-seconds:10}")
////    private long windowSeconds;
////
////    @Value("${rate-limit.key-prefix:unihub:gateway:ratelimit}")
////    private String keyPrefix;
////
////    public RedisSlidingWindowRateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
////        this.redisTemplate = redisTemplate;
////        this.slidingWindowScript = RedisScript.of(SLIDING_WINDOW_LUA, Long.class);
////    }
////
////    @Override
////    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
////        if (!enabled) {
////            return chain.filter(exchange);
////        }
////
////        String path = exchange.getRequest().getPath().value();
////        HttpMethod method = exchange.getRequest().getMethod();
////        String routeKey = extractRouteKey(path, method);
////        String principalKey = extractPrincipalKey(exchange);
////        String key = keyPrefix + ":" + routeKey + ":" + principalKey;
////
////        int effectiveMaxRequests = this.maxRequests; // Mặc định là 60
////        long effectiveWindowMillis = this.windowSeconds * 1000; // Mặc định là 10s
////
////        if ("action:registration".equals(routeKey)) {
////            effectiveMaxRequests = 5;
////            effectiveWindowMillis = 10000;
////        }
////
////        long nowMillis = System.currentTimeMillis();
////        String requestMember = nowMillis + "-" + UUID.randomUUID();
////
////        return redisTemplate.execute(
////                        slidingWindowScript,
////                        List.of(key),
////                        String.valueOf(nowMillis),
////                        String.valueOf(effectiveWindowMillis),
////                        String.valueOf(effectiveMaxRequests),
////                        requestMember
////                )
////                .next()
////                .defaultIfEmpty(1L)
////                .flatMap(result -> {
////                    if (result != null && result == 1L) {
////                        return chain.filter(exchange);
////                    }
////                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
////                    exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(windowSeconds));
////                    return exchange.getResponse().setComplete();
////                })
////                .onErrorResume(throwable -> chain.filter(exchange));
////    }
////
////    @Override
////    public int getOrder() {
////        return -110;
////    }
////
////    private String extractRouteKey(String path, HttpMethod method) {
////        if (path == null || path.isBlank()) {
////            return "root";
////        }
////
////        if (path.endsWith("/tickets") && HttpMethod.POST.equals(method)) {
////            return "action:registration";
////        }
////
////        String normalized = path.startsWith("/api/") ? path.substring(5) :
////                (path.startsWith("/") ? path.substring(1) : path);
////
////        String[] segments = normalized.split("/");
////        if (segments.length == 0 || segments[0].isBlank()) {
////            return "root";
////        }
////
////        if (segments.length > 1) {
////            return segments[0] + ":" + segments[1];
////        }
////        return segments[0];
////    }
////
////    private String extractPrincipalKey(ServerWebExchange exchange) {
////        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
////        if (userId != null && !userId.isBlank()) {
////            return "u:" + userId;
////        }
////
////        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
////        if (authHeader != null && !authHeader.isBlank()) {
////            return "t:" + sha256(authHeader);
////        }
////
////        String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
////                .map(address -> address.getAddress().getHostAddress())
////                .orElse("unknown");
////        return "ip:" + ip;
////    }
////
////    private String sha256(String value) {
////        try {
////            MessageDigest digest = MessageDigest.getInstance("SHA-256");
////            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
////            return HexFormat.of().formatHex(hash);
////        } catch (NoSuchAlgorithmException e) {
////            return Integer.toHexString(value.hashCode());
////        }
////    }
////}
