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

    private static final Map<String, List<String>> ROLE_REQUIREMENTS = Map.of(
            "^/api/admin/.*", List.of("HOST"),
            "^/api/staff/.*", List.of("HOST", "STAFF"),
            "^/api/workshops/.*", List.of("ATTENDEE","HOST", "STAFF")
    );


    public boolean isSecured(String path) {
        return OPEN_API_ENDPOINTS.stream()
                .noneMatch(path::contains);
    }


    public boolean isAuthorized(String path, String userRole) {

        for (Map.Entry<String, List<String>> entry : ROLE_REQUIREMENTS.entrySet()) {
            // Dùng Regex để match toàn bộ các đường dẫn con
            if (Pattern.matches(entry.getKey(), path)) {
                return entry.getValue().contains(userRole);
            }
        }
        return true;
    }
}