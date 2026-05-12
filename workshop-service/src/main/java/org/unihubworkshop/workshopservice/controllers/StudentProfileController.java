package org.unihubworkshop.workshopservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.unihubworkshop.workshopservice.common.ApiResponse;
import org.unihubworkshop.workshopservice.dto.StudentProfileResponse;
import org.unihubworkshop.workshopservice.services.ImportService;
import org.unihubworkshop.workshopservice.services.StudentProfileService;
import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final ImportService importService;

    public StudentProfileController(StudentProfileService studentProfileService, ImportService importService) {
        this.studentProfileService = studentProfileService;
        this.importService = importService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentProfileResponse>>> getStudents() {
        List<StudentProfileResponse> data = studentProfileService.getAllStudents();

        ApiResponse<List<StudentProfileResponse>> response =
                new ApiResponse<>(true, "Get all student profiles successfully", data);

        return ResponseEntity.ok(response);

    }


    @PostMapping("/import-csv")
    public ResponseEntity<ApiResponse<String>> importCsv(@RequestParam("file") MultipartFile file) {
        String message = importService.uploadAndPublish(file);
        ApiResponse<String> response = new ApiResponse<>(true, "File queued for import", message);
        return ResponseEntity.ok(response);
    }
}