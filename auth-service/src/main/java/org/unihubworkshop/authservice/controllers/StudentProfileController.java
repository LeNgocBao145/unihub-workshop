package org.unihubworkshop.authservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unihubworkshop.authservice.dto.PageResponse;
import org.unihubworkshop.authservice.dto.StudentProfileDto;
import org.unihubworkshop.authservice.entities.StudentProfile;
import org.unihubworkshop.authservice.services.StudentProfileService;
import org.unihubworkshop.authservice.specifications.StudentProfileSpecification;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping
    public ResponseEntity<PageResponse<StudentProfileDto>> getStudents(
            @RequestParam(required = false) String studentCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String className,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "studentCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<StudentProfile> spec = StudentProfileSpecification.filterBy(
                studentCode, department, major, className);

        Page<StudentProfile> studentPage = studentProfileService.findAll(spec, pageable);
        
        Page<StudentProfileDto> dtoPage = studentPage.map(StudentProfileDto::fromEntity);

        return ResponseEntity.ok(PageResponse.of(dtoPage));
    }
}