package org.unihubworkshop.apigateway.validators;


import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class RouteValidator {


    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/register",
            "/login",
            "/refresh-token"
    );

    // 2. Từ điển phân quyền: Regex của URI -> Danh sách Role hợp lệ
    // Bạn có thể mở rộng danh sách này thoải mái khi có thêm tính năng
    private static final Map<String, List<String>> ROLE_REQUIREMENTS = Map.of(
            "^/api/admin/.*", List.of("HOST"),
            "^/api/staff/.*", List.of("HOST", "STAFF"),
            "^/api/workshops/.*", List.of("ATTENDEE","HOST", "STAFF")
    );

    /**
     * Kiểm tra xem đường dẫn có nằm trong danh sách public không.
     * Trả về true nếu CẦN token (đường dẫn bảo mật).
     */
    public boolean isSecured(String path) {
        return OPEN_API_ENDPOINTS.stream()
                .noneMatch(path::contains);
    }

    /**
     * Kiểm tra xem Role của user có được phép truy cập đường dẫn hiện tại không.
     */
    public boolean isAuthorized(String path, String userRole) {
        for (Map.Entry<String, List<String>> entry : ROLE_REQUIREMENTS.entrySet()) {
            // Dùng Regex để match toàn bộ các đường dẫn con
            if (Pattern.matches(entry.getKey(), path)) {
                return entry.getValue().contains(userRole);
            }
        }
        // Nếu API cần token nhưng KHÔNG yêu cầu Role cụ thể (ví dụ: xem profile cá nhân),
        // thì mặc định cho phép đi qua.
        return true;
    }
}