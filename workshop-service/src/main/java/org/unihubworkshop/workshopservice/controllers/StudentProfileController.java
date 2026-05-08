package org.unihubworkshop.workshopservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unihubworkshop.workshopservice.common.ApiResponse;
import org.unihubworkshop.workshopservice.common.PageResponse;
import org.unihubworkshop.workshopservice.dto.StudentProfileResponse;
import org.unihubworkshop.workshopservice.services.StudentProfileService;
import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudentProfileResponse>>> getStudents(
           @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
                 PageResponse<StudentProfileResponse>  data = studentProfileService.getAllStudents(page - 1, size);

        ApiResponse<PageResponse<StudentProfileResponse> > response =
                new ApiResponse<>(true, "Get all student profiles successfully", data);

        return ResponseEntity.ok(response);

    }
}