package org.unihubworkshop.apigateway.validators;


import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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

    private static final Map<String, List<String>> ROLE_REQUIREMENTS = new LinkedHashMap<>();

    static {

        ROLE_REQUIREMENTS.put("^.* /admin/.*$", List.of("HOST"));
        ROLE_REQUIREMENTS.put("^.* /staff/.*$", List.of("HOST", "STAFF"));


        // QUY TẮC VÀNG: CÁC ROUTE ĐẶC THÙ / CỤ THỂ PHẢI ĐẶT LÊN TRÊN

        ROLE_REQUIREMENTS.put("^POST /workshops/.*/tickets$", List.of("ATTENDEE", "HOST", "STAFF"));
        ROLE_REQUIREMENTS.put("^GET /workshops/.*/tickets/[^/]+$", List.of("ATTENDEE", "HOST", "STAFF"));


        ROLE_REQUIREMENTS.put("^POST /workshops/check-in.*$", List.of("ATTENDEE", "HOST", "STAFF"));

        // CÁC ROUTE CHUNG CHUNG (GENERIC) PHẢI ĐẶT Ở DƯỚI CÙNG

        ROLE_REQUIREMENTS.put("^(POST|PUT|PATCH|DELETE) /workshops.*$", List.of("HOST"));
        ROLE_REQUIREMENTS.put("^GET /workshops.*$", List.of("ATTENDEE", "HOST", "STAFF"));

        ROLE_REQUIREMENTS.put("^GET /auth/users/me$", List.of("ATTENDEE", "HOST", "STAFF"));
        ROLE_REQUIREMENTS.put("^POST /auth/logout$", List.of("ATTENDEE", "HOST", "STAFF"));
        ROLE_REQUIREMENTS.put("^GET /students.*$", List.of("HOST"));
        ROLE_REQUIREMENTS.put("^POST /students/.*$", List.of("HOST"));
        ROLE_REQUIREMENTS.put("^GET /tickets/me$", List.of("ATTENDEE", "HOST", "STAFF"));
        ROLE_REQUIREMENTS.put("^GET /tickets.*$", List.of("HOST"));
    }

    public boolean isSecured(String path) {
        return OPEN_API_ENDPOINTS.stream()
                .noneMatch(path::contains);
    }


    public boolean isAuthorized(String method, String path, String userRole) {

        String routeKey = method.toUpperCase() + " " + path;

        for (Map.Entry<String, List<String>> entry : ROLE_REQUIREMENTS.entrySet()) {
            if (Pattern.matches(entry.getKey(), routeKey)) {
                return entry.getValue().contains(userRole);
            }
        }


        return false;
    }
}