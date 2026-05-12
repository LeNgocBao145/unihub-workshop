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

    private static final Map<String, List<String>> ROLE_REQUIREMENTS = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("^/admin/.*$", List.of("HOST")),
            Map.entry("^/staff/.*$", List.of("HOST", "STAFF")),
            // Ticket-specific patterns (most specific first)
            Map.entry("^GET /tickets/.+/status/stream$", List.of("ATTENDEE", "HOST", "STAFF")),
            Map.entry("^GET /tickets/me$", List.of("ATTENDEE", "HOST", "STAFF")),
            Map.entry("^GET /tickets.*$", List.of("ATTENDEE", "HOST", "STAFF")),
            // Workshop-specific patterns (most specific first)
            Map.entry("^POST /workshops/check-in.*$", List.of("ATTENDEE", "HOST", "STAFF")),
            Map.entry("^GET /workshops/.*/tickets/[^/]+$", List.of("ATTENDEE", "HOST", "STAFF")),
            Map.entry("^POST /workshops/.*/tickets$", List.of("ATTENDEE", "HOST", "STAFF")),
            Map.entry("^GET /workshops/.*/tickets$", List.of("ATTENDEE", "HOST", "STAFF")),
            // General patterns after specific ones
            Map.entry("^(POST|PUT|PATCH|DELETE) /workshops.*$", List.of("HOST")),
            Map.entry("^GET /workshops.*$", List.of("ATTENDEE", "HOST", "STAFF")),
            Map.entry("^GET /auth/users/me$", List.of("ATTENDEE", "HOST", "STAFF")),
            Map.entry("^POST /auth/logout$", List.of("ATTENDEE", "HOST", "STAFF")),
            Map.entry("^GET /students.*$", List.of("HOST")),
            Map.entry("^POST /students/.*$", List.of("HOST"))
    ));

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