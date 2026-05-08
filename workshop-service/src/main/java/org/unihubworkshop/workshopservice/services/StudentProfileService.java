package org.unihubworkshop.workshopservice.services;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.unihubworkshop.workshopservice.common.PageResponse;
import org.unihubworkshop.workshopservice.dto.RegistrationResponse;
import org.unihubworkshop.workshopservice.models.Registration;
import org.unihubworkshop.workshopservice.models.StudentProfile;
import org.unihubworkshop.workshopservice.models.Workshop;
import org.unihubworkshop.workshopservice.repositories.StudentProfileRepository;
import org.springframework.transaction.annotation.Transactional;
import org.unihubworkshop.workshopservice.dto.WorkshopResponse;
import org.unihubworkshop.workshopservice.mapper.StudentProfileMapper;
import org.unihubworkshop.workshopservice.dto.StudentProfileResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Service
@Transactional
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileMapper studentProfileMapper;
    
    public StudentProfileService(StudentProfileRepository studentProfileRepository, StudentProfileMapper studentProfileMapper) {
        this.studentProfileRepository = studentProfileRepository;
        this.studentProfileMapper = studentProfileMapper;
    }

 @Transactional(readOnly = true)
    public PageResponse<StudentProfileResponse> getAllStudents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
     Page<StudentProfile> studentProfilePage = studentProfileRepository.findAll(pageable);

     List<StudentProfileResponse> content = studentProfilePage.getContent()
             .stream()
             .map(studentProfileMapper::toResponse)
             .toList();

     return PageResponse.<StudentProfileResponse>builder()
             .content(content)
             .page(studentProfilePage.getNumber() + 1)
             .size(studentProfilePage.getSize())
             .totalElements(studentProfilePage.getTotalElements())
             .totalPages(studentProfilePage.getTotalPages())
             .hasNext(studentProfilePage.hasNext())
             .hasPrevious(studentProfilePage.hasPrevious())
             .build();

    }
}