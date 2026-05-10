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
            "^/admin/.*", List.of("HOST"),
            "^/staff/.*", List.of("HOST", "STAFF"),
            "^(POST|PUT|PATCH|DELETE) /workshops.*", List.of("HOST"),
            "^GET /workshops.*", List.of("ATTENDEE", "HOST", "STAFF"),
            "^GET /auth/users/me", List.of("ATTENDEE", "HOST", "STAFF"),
            "^POST /auth/logout", List.of("ATTENDEE", "HOST", "STAFF"),
            "^(POST|PUT|PATCH|GET) /ticket.*", List.of("ATTENDEE", "HOST", "STAFF")


    );


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