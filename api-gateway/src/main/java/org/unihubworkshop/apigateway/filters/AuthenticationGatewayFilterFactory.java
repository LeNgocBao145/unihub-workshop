package org.unihubworkshop.apigateway.filters;



import org.unihubworkshop.apigateway.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.unihubworkshop.apigateway.validators.RouteValidator;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RouteValidator routeValidator;
    public AuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    public static class Config {
    }

    // Giúp Spring hiểu cách map tham số từ yml vào Config
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("allowedRoles");
    }


    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // 1. Kiểm tra xem route có cần xác thực không
            if (!routeValidator.isSecured(path)) {
                return chain.filter(exchange);
            }

            // 2. Lấy và kiểm tra định dạng Token
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Thiếu token hoặc token không đúng định dạng", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            try {


                jwtUtil.validateToken(token);

                Claims claims = jwtUtil.getClaims(token);


                String userId = claims.getSubject();
                String role = claims.get("role", String.class);


                // 4. LOGIC RBAC CHÍNH: Ủy quyền cho RouteValidator kiểm tra
                if (!routeValidator.isAuthorized(path, role)) {
                    return onError(exchange, "Bạn không có quyền truy cập hệ thống này", HttpStatus.FORBIDDEN);
                }

                // 5. Đính header và cho request đi tiếp xuống Microservices
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Role", role)
                                .build())
                        .build();

                return chain.filter(mutatedExchange);
            } catch (Exception e) {
                return onError(exchange, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED);
            }

        });
    }


    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        // Lưu ý: Để chuyên nghiệp hơn, bạn có thể return chuỗi JSON báo lỗi ở đây
        return exchange.getResponse().setComplete();
    }
}
